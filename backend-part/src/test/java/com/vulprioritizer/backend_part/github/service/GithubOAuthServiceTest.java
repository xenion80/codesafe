package com.vulprioritizer.backend_part.github.service;

import com.vulprioritizer.backend_part.github.entity.GithubConnection;
import com.vulprioritizer.backend_part.github.repository.GithubConnectionRepository;
import com.vulprioritizer.backend_part.user.entity.User;
import com.vulprioritizer.backend_part.user.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.client.RestClient;

import javax.crypto.KeyGenerator;
import javax.crypto.SecretKey;
import java.security.NoSuchAlgorithmException;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class GithubOAuthServiceTest {

    @Mock
    private GithubConnectionRepository githubConnectionRepository;

    @Mock
    private UserRepository userRepository;

    @Mock
    private RestClient restClient;

    private GithubOAuthService githubOAuthService;

    private User testUser;
    private GithubConnection testConnection;

    @BeforeEach
    void setUp() throws NoSuchAlgorithmException {
        KeyGenerator keyGen = KeyGenerator.getInstance("HmacSHA256");
        SecretKey secretKey = keyGen.generateKey();

        githubOAuthService = new GithubOAuthService(
                githubConnectionRepository,
                restClient,
                userRepository,
                secretKey
        );

        ReflectionTestUtils.setField(githubOAuthService, "clientId", "test-client-id");
        ReflectionTestUtils.setField(githubOAuthService, "clientSecret", "test-client-secret");
        ReflectionTestUtils.setField(githubOAuthService, "redirectUri", "http://localhost:8080/integrations/github/callback");

        testUser = new User();
        testUser.setId(42L);
        testUser.setEmail("test@cybertotal.local");

        testConnection = new GithubConnection();
        testConnection.setId(1L);
        testConnection.setUser(testUser);
        testConnection.setGithubUserId(999L);
        testConnection.setGithubUsername("test-github-user");
        testConnection.setAccessToken("gho_test_token");
    }

    @Test
    void disconnect_whenConnectionExists_deletesConnection() {
        when(githubConnectionRepository.findByUser(testUser)).thenReturn(Optional.of(testConnection));

        githubOAuthService.disconnect(testUser);

        verify(githubConnectionRepository, times(1)).delete(testConnection);
    }

    @Test
    void disconnect_whenConnectionDoesNotExist_doesNotDelete() {
        when(githubConnectionRepository.findByUser(testUser)).thenReturn(Optional.empty());

        githubOAuthService.disconnect(testUser);

        verify(githubConnectionRepository, never()).delete(any());
    }

    @Test
    void getConnectedUser_returnsOptionalConnection() {
        when(githubConnectionRepository.findByUser(testUser)).thenReturn(Optional.of(testConnection));

        Optional<GithubConnection> result = githubOAuthService.getConnectedUser(testUser);

        assertThat(result).isPresent();
        assertThat(result.get().getGithubUsername()).isEqualTo("test-github-user");
    }
}
