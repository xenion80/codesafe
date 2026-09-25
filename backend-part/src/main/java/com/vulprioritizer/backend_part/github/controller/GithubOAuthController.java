package com.vulprioritizer.backend_part.github.controller;

import com.vulprioritizer.backend_part.github.service.GithubOAuthService;
import com.vulprioritizer.backend_part.user.entity.User;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.net.URI;

@RestController
@RequiredArgsConstructor
@RequestMapping("/integrations/github")
public class GithubOAuthController {

    private final GithubOAuthService githubOAuthService;

    /**
     * Where to send the browser after a successful connection. Empty by default (no
     * frontend yet): the backend then serves its own success page instead of
     * redirecting to a dead URL.
     */
    @Value("${app.frontend-url:}")
    private String frontendUrl;

    @GetMapping("/authorize")
    public ResponseEntity<Void> authorize() {
        Authentication authentication =
                SecurityContextHolder.getContext().getAuthentication();

        User user = (User) authentication.getPrincipal();

        // Signed, self-contained state (JWT): no server-side session needed, so the
        // flow survives client switches (API client -> browser, SPA -> browser).
        String state = githubOAuthService.generateState(user);

        String authorizationUrl =
                githubOAuthService.buildAuthorizationUrl(state);

        return ResponseEntity
                .status(302)
                .header(HttpHeaders.LOCATION, authorizationUrl)
                .build();
    }

    @GetMapping("/callback")
    public ResponseEntity<?> callback(
            @RequestParam String code,
            @RequestParam String state) {

        String githubUsername = githubOAuthService.handleCallback(code, state);

        if (frontendUrl == null || frontendUrl.isBlank()) {
            return ResponseEntity.ok()
                    .contentType(MediaType.TEXT_HTML)
                    .body(successPage(githubUsername));
        }
        return ResponseEntity
                .status(HttpStatus.FOUND)
                .location(URI.create(frontendUrl + "/settings/integrations?connected=github"))
                .build();
    }

    @GetMapping("/repos")
    public ResponseEntity<?> getRepositories() {

        return ResponseEntity.ok(
                githubOAuthService.getRepositories()
        );
    }

    /** Minimal, dependency-free success page shown while there is no frontend. */
    private String successPage(String githubUsername) {
        return "<!DOCTYPE html><html><head><meta charset=\"utf-8\">"
                + "<title>GitHub connected</title>"
                + "<style>body{font-family:system-ui,sans-serif;background:#0d1117;color:#e6edf3;"
                + "display:flex;align-items:center;justify-content:center;height:100vh;margin:0}"
                + "div{text-align:center}h1{color:#3fb950}code{background:#161b22;padding:2px 8px;"
                + "border-radius:6px}</style></head><body><div>"
                + "<h1>&#10003; GitHub connected</h1>"
                + "<p>Connected as <code>" + escapeHtml(githubUsername) + "</code></p>"
                + "<p>You can close this tab and return to your API client.</p>"
                + "</div></body></html>";
    }

    private String escapeHtml(String value) {
        if (value == null) {
            return "";
        }
        return value.replace("&", "&amp;").replace("<", "&lt;")
                .replace(">", "&gt;").replace("\"", "&quot;");
    }
}
