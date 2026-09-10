package com.vulprioritizer.backend_part.github.service;

import com.vulprioritizer.backend_part.github.dto.response.GithubRepositoryResponse;
import com.vulprioritizer.backend_part.github.dto.response.GithubTokenResponse;
import com.vulprioritizer.backend_part.github.entity.GithubConnection;
import com.vulprioritizer.backend_part.github.repository.GithubConnectionRepository;
import com.vulprioritizer.backend_part.user.entity.User;
import com.vulprioritizer.backend_part.user.repository.UserRepository;
import com.vulprioritizer.backend_part.user.service.UserService;
import jakarta.servlet.http.HttpSession;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class GithubOAuthService {

    @Value("${github.client-id}")
    private String clientId;

    @Value("${github.redirect-uri}")
    private String redirectUri;

    @Value("${github.client-secret}")
    private String clientSecret;

    private final GithubConnectionRepository githubConnectionRepository;

    private final RestClient restClient;

    private final UserRepository userRepository;

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

    public String generateState() {
        return UUID.randomUUID().toString();
    }

    public void handleCallback(
            String code,
            String state,
            HttpSession session
    ) {
        String savedState =
                (String) session.getAttribute("github_oauth_state");

        if (savedState == null || !savedState.equals(state)) {
            throw new IllegalArgumentException("Invalid OAuth state");
        }

        Long userId =
                (Long) session.getAttribute("github_oauth_user_id");

        if (userId == null) {
            throw new IllegalStateException("User session not found");
        }

        session.removeAttribute("github_oauth_state");
        session.removeAttribute("github_oauth_user_id");

        GithubTokenResponse tokenResponse =
                restClient.post()
                        .uri("https://github.com/login/oauth/access_token")
                        .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                        .body(
                                "client_id=" + encode(clientId)
                                        + "&client_secret=" + encode(clientSecret)
                                        + "&code=" + encode(code)
                                        + "&redirect_uri=" + encode(redirectUri)
                        )
                        .retrieve()
                        .body(GithubTokenResponse.class);

        String accessToken = tokenResponse.getAccessToken();
        User user = userRepository.findById(userId)
                .orElseThrow(() ->
                        new UsernameNotFoundException("User not found")
                );

        GithubConnection connection = new GithubConnection();
        connection.setUser(user);
        connection.setAccessToken(accessToken);

        githubConnectionRepository.save(connection);
    }

    public List<GithubRepositoryResponse> getRepositories() {

        String accessToken = getCurrentUserGithubToken();

        return restClient.get()
                .uri("https://api.github.com/user/repos")
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

        GithubConnection integration =
                githubConnectionRepository
                        .findByUser(user)
                        .orElseThrow(
                                () -> new RuntimeException(
                                        "GitHub account not connected"
                                )
                        );

        return integration.getAccessToken();
    }

}