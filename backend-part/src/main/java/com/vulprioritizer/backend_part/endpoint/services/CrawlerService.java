package com.vulprioritizer.backend_part.endpoint.services;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

@Service
public class CrawlerService {

    private static final Logger log = LoggerFactory.getLogger(CrawlerService.class);

    static final int MAX_DEPTH = 3;
    static final int MAX_PAGES = 50;

    public record DiscoveredUrl(String url, int depth) {
    }

    public enum CrawlStatus {
        COMPLETED,
        MAX_PAGES_REACHED,
        MAX_DEPTH_REACHED,
        START_URL_UNREACHABLE,
        FAILED
    }

    public record CrawlResult(CrawlStatus status, List<DiscoveredUrl> discoveredUrls, int pagesVisited, int failedRequests) {
    }

    private static final class FetchException extends Exception {
        private final PageFetcher.FetchStatus status;

        private FetchException(PageFetcher.FetchStatus status) {
            super(status.name());
            this.status = status;
        }
    }

    private final PageFetcher pageFetcher;

    public CrawlerService(PageFetcher pageFetcher) {
        this.pageFetcher = pageFetcher;
    }

    public CrawlResult crawl(String baseUrl) {
        long start = System.currentTimeMillis();

        URI baseUri = parse(baseUrl);
        if (baseUri == null || baseUri.getHost() == null) {
            log.warn("Crawler: target base URL is not a usable absolute URL: {}", baseUrl);
            return new CrawlResult(CrawlStatus.FAILED, List.of(), 0, 0);
        }
        String origin = originOf(baseUri);

        Set<String> visited = new LinkedHashSet<>();
        List<DiscoveredUrl> discovered = new ArrayList<>();
        Deque<DiscoveredUrl> queue = new ArrayDeque<>();
        int failedRequests = 0;

        queue.add(new DiscoveredUrl(normalize(baseUri), 0));

        while (!queue.isEmpty() && visited.size() < MAX_PAGES) {
            DiscoveredUrl current = queue.poll();
            if (!visited.add(current.url())) {
                continue;
            }

            String body;
            try {
                body = fetchBody(current.url());
            } catch (FetchException e) {
                failedRequests++;
                log.debug("Crawler: skipping {} ({})", current.url(), e.status);
                continue;
            }

            discovered.add(current);
            if (current.depth() >= MAX_DEPTH) {
                continue;
            }

            for (String rawHref : extractLinks(body)) {
                URI resolved = resolve(rawHref, current.url());
                if (resolved == null || !isSameOrigin(resolved, origin)) {
                    continue;
                }
                if (resolved.getRawPath() != null && hasFileExtension(resolved.getRawPath())) {
                    continue;
                }
                String normalized = normalize(resolved);
                if (visited.contains(normalized) || containsUrl(queue, normalized)) {
                    continue;
                }
                queue.add(new DiscoveredUrl(normalized, current.depth() + 1));
            }
        }

        CrawlStatus status = CrawlStatus.COMPLETED;
        if (visited.size() >= MAX_PAGES) {
            status = CrawlStatus.MAX_PAGES_REACHED;
        } else if (discovered.isEmpty() && !visited.isEmpty()) {
            status = CrawlStatus.START_URL_UNREACHABLE;
        }

        log.info("Crawler: {} visited, {} discovered, {} failed requests in {} ms (base: {})",
                visited.size(), discovered.size(), failedRequests,
                System.currentTimeMillis() - start, baseUrl);
        return new CrawlResult(status, discovered, visited.size(), failedRequests);
    }

    private boolean containsUrl(Deque<DiscoveredUrl> queue, String url) {
        return queue.stream().anyMatch(d -> d.url().equals(url));
    }

    private String fetchBody(String url) throws FetchException {
        PageFetcher.PageResult result = pageFetcher.fetch(url);
        switch (result.status()) {
            case SUCCESS -> {
                if (result.body() == null) {
                    throw new FetchException(PageFetcher.FetchStatus.CONNECTION_ERROR);
                }
                return result.body();
            }
            case HTTP_ERROR, TIMEOUT, CONNECTION_ERROR, INVALID_URL ->
                    throw new FetchException(result.status());
        }
        throw new FetchException(PageFetcher.FetchStatus.CONNECTION_ERROR);
    }

    List<String> extractLinks(String html) {
        Document document = Jsoup.parse(html == null ? "" : html);
        List<String> links = new ArrayList<>();
        document.select("a[href]").forEach(element -> {
            String href = element.attr("href").trim();
            if (!href.isEmpty() && !href.startsWith("#")) {
                links.add(href);
            }
        });
        return links;
    }

    URI resolve(String href, String pageUrl) {
        if (href == null || href.isBlank()) {
            return null;
        }
        try {
            URI page = URI.create(pageUrl);
            URI resolved = page.resolve(href.trim());
            String scheme = resolved.getScheme() == null ? null : resolved.getScheme().toLowerCase(Locale.ROOT);
            if (scheme == null || !(scheme.equals("http") || scheme.equals("https"))) {
                return null;
            }
            if (resolved.getHost() == null) {
                return null;
            }
            return resolved;
        } catch (IllegalArgumentException e) {
            return null;
        }
    }

    boolean isSameOrigin(URI uri, String origin) {
        return origin.equals(originOf(uri));
    }

    private String originOf(URI uri) {
        String scheme = uri.getScheme() == null ? "" : uri.getScheme().toLowerCase(Locale.ROOT);
        String host = uri.getHost() == null ? "" : uri.getHost().toLowerCase(Locale.ROOT);
        int port = uri.getPort();
        int effective;
        if (port == -1) {
            effective = switch (scheme) {
                case "https" -> 443;
                case "http" -> 80;
                default -> -1;
            };
        } else {
            effective = port;
        }
        if ("http".equals(scheme) && effective == 80 || "https".equals(scheme) && effective == 443) {
            effective = -1;
        }
        return effective == -1 ? scheme + "://" + host : scheme + "://" + host + ":" + effective;
    }

    String normalize(URI uri) {
        String scheme = uri.getScheme() == null ? "" : uri.getScheme().toLowerCase(Locale.ROOT);
        String host = uri.getHost() == null ? "" : uri.getHost().toLowerCase(Locale.ROOT);

        int port = uri.getPort();
        String portPart = "";
        if (port != -1 && !(("http".equals(scheme) && port == 80) || ("https".equals(scheme) && port == 443))) {
            portPart = ":" + port;
        }

        String path = uri.getRawPath() == null || uri.getRawPath().isEmpty()
                ? "/"
                : removeDotSegments(uri.getRawPath());

        if (!path.endsWith("/") && !hasFileExtension(path)) {
            path = path + "/";
        }

        String query = uri.getRawQuery();
        return scheme + "://" + host + portPart + path + (query == null || query.isEmpty() ? "" : "?" + query);
    }

    String normalize(String url) {
        URI uri = parse(url);
        return uri == null ? url : normalize(uri);
    }

    private URI parse(String url) {
        try {
            return new URI(url.trim());
        } catch (URISyntaxException | NullPointerException e) {
            return null;
        }
    }

    private boolean hasFileExtension(String path) {
        String lastSegment = path.substring(path.lastIndexOf('/') + 1);
        int dot = lastSegment.lastIndexOf('.');
        if (dot <= 0) {
            return false;
        }
        String ext = lastSegment.substring(dot + 1).toLowerCase(Locale.ROOT);
        return Set.of("jpg", "jpeg", "png", "gif", "svg", "css", "js", "ico",
                "pdf", "zip", "mp4", "mp3").contains(ext);
    }

    private static String removeDotSegments(String path) {
        boolean trailingSlash = path.endsWith("/");
        String[] segments = path.split("/");
        Deque<String> stack = new ArrayDeque<>();
        for (String segment : segments) {
            if (segment.isEmpty() || segment.equals(".")) {
                continue;
            }
            if (segment.equals("..")) {
                if (!stack.isEmpty()) {
                    stack.removeLast();
                }
            } else {
                stack.addLast(segment);
            }
        }
        StringBuilder result = new StringBuilder("/");
        while (!stack.isEmpty()) {
            result.append(stack.removeFirst());
            if (!stack.isEmpty()) {
                result.append('/');
            }
        }
        if (trailingSlash && result.charAt(result.length() - 1) != '/') {
            result.append('/');
        }
        return result.toString();
    }
}
