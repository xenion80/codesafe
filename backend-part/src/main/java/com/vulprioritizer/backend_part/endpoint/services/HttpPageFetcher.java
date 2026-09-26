package com.vulprioritizer.backend_part.endpoint.services;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.net.http.HttpTimeoutException;
import java.time.Duration;
import java.util.Locale;
import java.util.Set;

@Component
public class HttpPageFetcher implements PageFetcher {

    private static final Logger log = LoggerFactory.getLogger(HttpPageFetcher.class);

    static final Duration REQUEST_TIMEOUT = Duration.ofSeconds(10);
    private static final Duration CONNECT_TIMEOUT = Duration.ofSeconds(10);

    private static final int MAX_REDIRECT_HOPS = 5;

    private static final Set<String> ALLOWED_SCHEMES = Set.of("http", "https");

    private final HttpClient httpClient;

    public HttpPageFetcher() {
        this(HttpClient.newBuilder()
                .followRedirects(HttpClient.Redirect.NEVER)
                .connectTimeout(CONNECT_TIMEOUT)
                .build());
    }

    HttpPageFetcher(HttpClient httpClient) {
        this.httpClient = httpClient;
    }

    @Override
    public PageResult fetch(String url) {
        URI current = parseAllowed(url);
        if (current == null) {
            return PageResult.failure(FetchStatus.INVALID_URL, -1);
        }
        URI original = current;

        for (int hop = 0; hop <= MAX_REDIRECT_HOPS; hop++) {
            HttpRequest request;
            try {
                request = HttpRequest.newBuilder(current)
                        .timeout(REQUEST_TIMEOUT)
                        .header("Accept", "text/html,application/xhtml+xml")
                        .header("User-Agent", "VulPrioritizer-Crawler/1.0 (endpoint discovery)")
                        .GET()
                        .build();
            } catch (IllegalArgumentException e) {
                return PageResult.failure(FetchStatus.INVALID_URL, -1);
            }

            HttpResponse<String> response;
            try {
                response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
            } catch (HttpTimeoutException e) {
                log.debug("Crawler request timed out: {}", url);
                return PageResult.failure(FetchStatus.TIMEOUT, -1);
            } catch (IOException e) {
                log.debug("Crawler request failed for {}: {}", url, e.getMessage());
                return PageResult.failure(FetchStatus.CONNECTION_ERROR, -1);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                return PageResult.failure(FetchStatus.CONNECTION_ERROR, -1);
            }

            int statusCode = response.statusCode();
            if (statusCode >= 200 && statusCode < 300) {
                return PageResult.success(statusCode, response.body());
            }

            if (statusCode >= 300 && statusCode < 400) {
                String location = response.headers().firstValue("Location").orElse(null);
                URI next = location == null ? null : safeResolve(current, location);
                if (next == null || !isAllowedScheme(next) || !isSameOrigin(next, original)) {
                    log.debug("Crawler: not following redirect from {} to {}", url, location);
                    return PageResult.failure(FetchStatus.HTTP_ERROR, statusCode);
                }
                current = next;
                continue;
            }

            return PageResult.failure(FetchStatus.HTTP_ERROR, statusCode);
        }

        log.debug("Crawler: too many redirects for {}", url);
        return PageResult.failure(FetchStatus.HTTP_ERROR, -1);
    }

    private URI parseAllowed(String url) {
        if (url == null) {
            return null;
        }
        try {
            URI uri = new URI(url.trim());
            return isAllowedScheme(uri) && uri.getHost() != null ? uri : null;
        } catch (URISyntaxException | IllegalArgumentException e) {
            return null;
        }
    }

    private boolean isAllowedScheme(URI uri) {
        return uri.getScheme() != null
                && ALLOWED_SCHEMES.contains(uri.getScheme().toLowerCase(Locale.ROOT));
    }

    private URI safeResolve(URI base, String location) {
        try {
            return base.resolve(location.trim());
        } catch (IllegalArgumentException e) {
            return null;
        }
    }

    private static boolean isSameOrigin(URI a, URI b) {
        return originOf(a).equals(originOf(b));
    }

    private static String originOf(URI uri) {
        String scheme = uri.getScheme() == null ? "" : uri.getScheme().toLowerCase(Locale.ROOT);
        String host = uri.getHost() == null ? "" : uri.getHost().toLowerCase(Locale.ROOT);
        int port = uri.getPort();
        if (port == -1) {
            port = switch (scheme) {
                case "https" -> 443;
                case "http" -> 80;
                default -> -2;
            };
        }
        if ((("http".equals(scheme) && port == 80)) || ("https".equals(scheme) && port == 443)) {
            port = -2;
        }
        return port == -2 ? scheme + "://" + host : scheme + "://" + host + ":" + port;
    }
}
