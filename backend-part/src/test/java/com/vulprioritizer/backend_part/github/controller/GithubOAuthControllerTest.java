package com.vulprioritizer.backend_part.github.controller;

import com.vulprioritizer.backend_part.github.entity.GithubConnection;
import com.vulprioritizer.backend_part.github.service.GithubOAuthService;
import com.vulprioritizer.backend_part.user.entity.User;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;

import java.time.LocalDateTime;
import java.util.Map;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class GithubOAuthControllerTest {

    @Mock
    private GithubOAuthService githubOAuthService;

    @InjectMocks
    private GithubOAuthController githubOAuthController;

    private User testUser;

    @BeforeEach
    void setUp() {
        testUser = new User();
        testUser.setId(42L);
        testUser.setEmail("test@cybertotal.local");

        UsernamePasswordAuthenticationToken authentication =
                new UsernamePasswordAuthenticationToken(testUser, null, testUser.getAuthorities());
        SecurityContextHolder.getContext().setAuthentication(authentication);
    }

    @Test
    void disconnect_callsServiceAndReturnsSuccess() {
        ResponseEntity<?> response = githubOAuthController.disconnect();

        verify(githubOAuthService, times(1)).disconnect(testUser);
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);

        @SuppressWarnings("unchecked")
        Map<String, Object> body = (Map<String, Object>) response.getBody();
        assertThat(body).isNotNull();
        assertThat(body.get("connected")).isEqualTo(false);
        assertThat(body.get("message")).isEqualTo("GitHub account disconnected successfully");
    }

    @Test
    void disconnectPost_callsServiceAndReturnsSuccess() {
        ResponseEntity<?> response = githubOAuthController.disconnectPost();

        verify(githubOAuthService, times(1)).disconnect(testUser);
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
    }

    @Test
    void getStatus_whenConnected_returnsConnectedTrue() {
        GithubConnection connection = new GithubConnection();
        connection.setGithubUsername("octocat");
        connection.setConnectedAt(LocalDateTime.of(2026, 1, 1, 12, 0));

        when(githubOAuthService.getConnectedUser(testUser)).thenReturn(Optional.of(connection));

        ResponseEntity<?> response = githubOAuthController.getStatus();

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        @SuppressWarnings("unchecked")
        Map<String, Object> body = (Map<String, Object>) response.getBody();
        assertThat(body).isNotNull();
        assertThat(body.get("connected")).isEqualTo(true);
        assertThat(body.get("username")).isEqualTo("octocat");
    }

    @Test
    void getStatus_whenNotConnected_returnsConnectedFalse() {
        when(githubOAuthService.getConnectedUser(testUser)).thenReturn(Optional.empty());

        ResponseEntity<?> response = githubOAuthController.getStatus();

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        @SuppressWarnings("unchecked")
        Map<String, Object> body = (Map<String, Object>) response.getBody();
        assertThat(body).isNotNull();
        assertThat(body.get("connected")).isEqualTo(false);
    }

    @Test
    void getAuthorizationUrl_returnsGeneratedUrl() {
        when(githubOAuthService.generateState(testUser)).thenReturn("dummy-state");
        when(githubOAuthService.buildAuthorizationUrl("dummy-state"))
                .thenReturn("https://github.com/login/oauth/authorize?state=dummy-state");

        ResponseEntity<?> response = githubOAuthController.getAuthorizationUrl();

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        @SuppressWarnings("unchecked")
        Map<String, Object> body = (Map<String, Object>) response.getBody();
        assertThat(body).isNotNull();
        assertThat(body.get("url")).isEqualTo("https://github.com/login/oauth/authorize?state=dummy-state");
    }

    @Test
    void callback_returnsSuccessHtmlPage() {
        when(githubOAuthService.handleCallback("auth-code", "valid-state")).thenReturn("octocat");

        ResponseEntity<?> response = githubOAuthController.callback("auth-code", "valid-state");

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getHeaders().getContentType()).isEqualTo(org.springframework.http.MediaType.TEXT_HTML);
        assertThat(response.getBody()).isInstanceOf(String.class);
        String html = (String) response.getBody();
        assertThat(html).contains("octocat");
        assertThat(html).contains("GITHUB_OAUTH_SUCCESS");
    }
}

