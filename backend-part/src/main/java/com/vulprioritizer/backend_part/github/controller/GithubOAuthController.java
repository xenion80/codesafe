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

    @Value("${app.frontend-url:}")
    private String frontendUrl;

    @GetMapping("/authorize")
    public ResponseEntity<Void> authorize() {
        Authentication authentication =
                SecurityContextHolder.getContext().getAuthentication();

        User user = (User) authentication.getPrincipal();

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

    @GetMapping("/url")
    public ResponseEntity<?> getAuthorizationUrl() {
        Authentication authentication =
                SecurityContextHolder.getContext().getAuthentication();
        User user = (User) authentication.getPrincipal();
        String state = githubOAuthService.generateState(user);
        String authorizationUrl = githubOAuthService.buildAuthorizationUrl(state);
        return ResponseEntity.ok(java.util.Map.of("url", authorizationUrl));
    }

    @GetMapping("/status")
    public ResponseEntity<?> getStatus() {
        Authentication authentication =
                SecurityContextHolder.getContext().getAuthentication();
        User user = (User) authentication.getPrincipal();
        return githubOAuthService.getConnectedUser(user)
                .map(conn -> ResponseEntity.ok(java.util.Map.of(
                        "connected", true,
                        "username", conn.getGithubUsername() != null ? conn.getGithubUsername() : "",
                        "connectedAt", conn.getConnectedAt() != null ? conn.getConnectedAt().toString() : ""
                )))
                .orElseGet(() -> ResponseEntity.ok(java.util.Map.of(
                        "connected", false,
                        "username", "",
                        "message", "GitHub not connected"
                )));
    }

    @GetMapping("/repos")
    public ResponseEntity<?> getRepositories() {

        return ResponseEntity.ok(
                githubOAuthService.getRepositories()
        );
    }

    private String successPage(String githubUsername) {
        return "<!DOCTYPE html><html><head><meta charset=\"utf-8\">"
                + "<title>GitHub connected</title>"
                + "<style>body{font-family:system-ui,-apple-system,sans-serif;background:#070b13;color:#e6edf3;"
                + "display:flex;align-items:center;justify-content:center;height:100vh;margin:0}"
                + ".card{text-align:center;background:#0d1527;padding:3rem 2.5rem;border-radius:16px;border:1px solid #1e293b;box-shadow:0 10px 40px rgba(0,0,0,0.5);max-width:440px;width:90%}"
                + "h1{color:#38bdf8;font-size:1.75rem;margin-bottom:0.75rem}"
                + "p{color:#94a3b8;margin:0.5rem 0}"
                + "code{background:#1e293b;color:#38bdf8;padding:4px 10px;border-radius:6px;font-weight:600;font-size:1rem}"
                + ".btn{display:inline-block;margin-top:1.5rem;background:#38bdf8;color:#070b13;font-weight:600;padding:10px 24px;border-radius:8px;text-decoration:none;cursor:pointer;border:none}"
                + "</style></head><body><div class=\"card\">"
                + "<h1>&#10003; GitHub Connected</h1>"
                + "<p>Connected as <code>" + escapeHtml(githubUsername) + "</code></p>"
                + "<p>You can close this tab or return to CyberTotal.</p>"
                + "<button class=\"btn\" onclick=\"returnToApp()\">Return to CyberTotal</button>"
                + "</div>"
                + "<script>"
                + "function returnToApp() {"
                + "  if (window.opener) { window.close(); } else { window.location.href = '/'; }"
                + "}"
                + "try {"
                + "  if (window.opener) {"
                + "    window.opener.postMessage({ type: 'GITHUB_OAUTH_SUCCESS', username: '" + escapeHtml(githubUsername) + "' }, '*');"
                + "    setTimeout(function() { window.close(); }, 1200);"
                + "  }"
                + "} catch(e) {}"
                + "</script>"
                + "</body></html>";
    }

    private String escapeHtml(String value) {
        if (value == null) {
            return "";
        }
        return value.replace("&", "&amp;").replace("<", "&lt;")
                .replace(">", "&gt;").replace("\"", "&quot;");
    }
}
