package com.vulprioritizer.backend_part.github.service;

import com.vulprioritizer.backend_part.common.exception.ResourceNotFoundException;
import com.vulprioritizer.backend_part.github.dto.response.GithubRepositoryResponse;
import com.vulprioritizer.backend_part.github.dto.response.GithubTokenResponse;
import com.vulprioritizer.backend_part.github.dto.response.GithubUserResponse;
import com.vulprioritizer.backend_part.github.entity.GithubConnection;
import com.vulprioritizer.backend_part.github.repository.GithubConnectionRepository;
import com.vulprioritizer.backend_part.user.entity.User;
import com.vulprioritizer.backend_part.user.repository.UserRepository;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

import javax.crypto.SecretKey;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.util.Date;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class GithubOAuthService {

    private static final long STATE_TTL_MS = 1000 * 60 * 10;

    @Value("${github.client-id}")
    private String clientId;

    @Value("${github.redirect-uri}")
    private String redirectUri;

    @Value("${github.client-secret}")
    private String clientSecret;

    private final GithubConnectionRepository githubConnectionRepository;
    private final RestClient restClient;
    private final UserRepository userRepository;
    private final SecretKey secretKey;

    public String buildAuthorizationUrl(String state) {

        String scope = "repo user:email";

        return "https://github.com/login/oauth/authorize"
                + "?client_id=" + encode(clientId)
                + "&redirect_uri=" + encode(redirectUri)
                + "&scope=" + encode(scope)
                + "&state=" + encode(state);
    }

    private String encode(String value) {
        return URLEncoder.encode(
                value,
                StandardCharsets.UTF_8
        );
    }

    public String generateState(User user) {
        return Jwts.builder()
                .subject(user.getId().toString())
                .claim("type", "oauth_state")
                .claim("nonce", UUID.randomUUID().toString())
                .issuedAt(new Date())
                .expiration(new Date(System.currentTimeMillis() + STATE_TTL_MS))
                .signWith(secretKey)
                .compact();
    }

    public String handleCallback(
            String code,
            String state
    ) {

        Long userId = validateStateAndGetUserId(state);

        GithubTokenResponse tokenResponse;
        try {
            tokenResponse = restClient.post()
                    .uri("https://github.com/login/oauth/access_token")
                    .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                    .accept(MediaType.APPLICATION_JSON)
                    .body(
                            "client_id=" + encode(clientId)
                                    + "&client_secret=" + encode(clientSecret)
                                    + "&code=" + encode(code)
                                    + "&redirect_uri=" + encode(redirectUri)
                    )
                    .retrieve()
                    .body(GithubTokenResponse.class);
        } catch (RestClientException e) {
            Throwable cause = e.getMostSpecificCause();
            log.error("GitHub token exchange failed for github.com/login/oauth/access_token: {}",
                    cause.getMessage(), e);
            throw new IllegalStateException(
                    "Could not reach GitHub to exchange the authorization code "
                            + "(connection failed: " + cause.getMessage() + "). "
                            + "Check the server's internet connection or proxy settings.");
        }

        if (tokenResponse == null
                || tokenResponse.getAccessToken() == null
                || tokenResponse.getAccessToken().isBlank()) {
            throw new IllegalStateException(
                    "GitHub access token was not received"
            );
        }

        String accessToken = tokenResponse.getAccessToken();

        GithubUserResponse githubUser;
        try {
            githubUser = restClient.get()
                    .uri("https://api.github.com/user")
                    .header(
                            HttpHeaders.AUTHORIZATION,
                            "Bearer " + accessToken
                    )
                    .header(
                            HttpHeaders.ACCEPT,
                            "application/vnd.github+json"
                    )
                    .retrieve()
                    .body(GithubUserResponse.class);
        } catch (RestClientException e) {
            Throwable cause = e.getMostSpecificCause();
            log.error("GitHub profile fetch failed for api.github.com/user: {}",
                    cause.getMessage(), e);
            throw new IllegalStateException(
                    "Could not reach GitHub to fetch the user profile "
                            + "(connection failed: " + cause.getMessage() + "). "
                            + "Check the server's internet connection or proxy settings.");
        }

        if (githubUser == null
                || githubUser.getId() == null
                || githubUser.getLogin() == null) {
            throw new IllegalStateException(
                    "Could not retrieve GitHub user profile"
            );
        }

        User user = userRepository.findById(userId)
                .orElseThrow(() ->
                        new UsernameNotFoundException("User not found")
                );

        GithubConnection connection = githubConnectionRepository
                .findByUser(user)
                .orElseGet(GithubConnection::new);

        connection.setUser(user);
        connection.setAccessToken(accessToken);
        connection.setGithubUserId(githubUser.getId());
        connection.setGithubUsername(githubUser.getLogin());
        connection.setConnectedAt(LocalDateTime.now());
        connection.setExpiresAt(
                tokenResponse.getExpiresIn() != null
                        ? LocalDateTime.now().plusSeconds(tokenResponse.getExpiresIn())
                        : null
        );

        githubConnectionRepository.save(connection);
        log.debug("GitHub OAuth state validated and connection saved for user {}", userId);
        return connection.getGithubUsername();
    }

    private Long validateStateAndGetUserId(String state) {
        Claims claims;
        try {
            claims = Jwts.parser()
                    .verifyWith(secretKey)
                    .build()
                    .parseSignedClaims(state)
                    .getPayload();
        } catch (ExpiredJwtException e) {
            log.warn("GitHub OAuth state rejected: expired (consent took longer than {} minutes). "
                    + "Restart the /integrations/github/authorize flow.", STATE_TTL_MS / 60000);
            throw new IllegalArgumentException(
                    "OAuth state expired - restart the GitHub connection flow ("
                            + "GET /integrations/github/authorize, then complete consent within "
                            + (STATE_TTL_MS / 60000) + " minutes)");
        } catch (JwtException | IllegalArgumentException e) {
            log.warn("GitHub OAuth state rejected: signature/type check failed ({}) - the callback "
                    + "must reach the SAME backend (same JWT_SECRET) that generated the state", e.getMessage());
            throw new IllegalArgumentException(
                    "Invalid OAuth state (signature check failed). The backend receiving the GitHub "
                            + "callback must be the same instance that served /integrations/github/authorize "
                            + "(same JWT_SECRET), and github.redirect-uri (GITHUB_REDIRECT_URI) must point at it");
        }
        if (!"oauth_state".equals(claims.get("type", String.class))) {
            log.warn("GitHub OAuth state rejected: wrong token type '{}'", claims.get("type", String.class));
            throw new IllegalArgumentException("Invalid OAuth state");
        }
        try {
            return Long.parseLong(claims.getSubject());
        } catch (NumberFormatException e) {
            throw new IllegalArgumentException("Invalid OAuth state");
        }
    }

    public List<GithubRepositoryResponse> getRepositories() {

        String accessToken = getCurrentUserGithubToken();

        return restClient.get()
                .uri("https://api.github.com/user/repos?per_page=100&sort=updated")
                .header(
                        HttpHeaders.AUTHORIZATION,
                        "Bearer " + accessToken
                )
                .header(
                        HttpHeaders.ACCEPT,
                        "application/vnd.github+json"
                )
                .retrieve()
                .body(new ParameterizedTypeReference<>() {});
    }

    private String getCurrentUserGithubToken() {

        Authentication authentication =
                SecurityContextHolder
                        .getContext()
                        .getAuthentication();

        User user = (User) authentication.getPrincipal();

        GithubConnection integration = githubConnectionRepository
                .findByUser(user)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "GitHub account not connected. Connect your GitHub account first"));

        return integration.getAccessToken();
    }

    public java.util.Optional<GithubConnection> getConnectedUser(User user) {
        return githubConnectionRepository.findByUser(user);
    }
}

