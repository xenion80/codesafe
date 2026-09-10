package com.vulprioritizer.backend_part.github.controller;


import com.vulprioritizer.backend_part.github.service.GithubOAuthService;
import com.vulprioritizer.backend_part.user.entity.User;
import jakarta.servlet.http.HttpSession;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

@RestController
@RequiredArgsConstructor
@RequestMapping("/integrations/github")
public class GithubOAuthController {

    private final GithubOAuthService githubOAuthService;

    @GetMapping("/authorize")
    public ResponseEntity<Void> authorize(
            HttpSession session
    ) {
        Authentication authentication =
                SecurityContextHolder.getContext().getAuthentication();

        User user = (User) authentication.getPrincipal();

        String state = githubOAuthService.generateState();

        session.setAttribute("github_oauth_state", state);
        session.setAttribute("github_oauth_user_id", user.getId());

        String authorizationUrl =
                githubOAuthService.buildAuthorizationUrl(state);

        return ResponseEntity
                .status(302)
                .header(HttpHeaders.LOCATION, authorizationUrl)
                .build();
    }

    @GetMapping("/callback")
    public ResponseEntity<String> callback(
            @RequestParam String code,
            @RequestParam String state,
            HttpSession session
    ) {

        githubOAuthService.handleCallback(code, state, session);

        return ResponseEntity.ok("GitHub connected successfully");
    }

    @GetMapping("/repos")
    public ResponseEntity<?> getRepositories() {

        return ResponseEntity.ok(
                githubOAuthService.getRepositories()
        );
    }
}