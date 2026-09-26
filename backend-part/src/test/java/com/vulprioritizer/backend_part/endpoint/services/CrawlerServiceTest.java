package com.vulprioritizer.backend_part.endpoint.services;

import com.vulprioritizer.backend_part.endpoint.services.PageFetcher.FetchStatus;
import com.vulprioritizer.backend_part.endpoint.services.PageFetcher.PageResult;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;
import java.util.HashMap;
import java.util.function.Function;
import java.util.stream.Collectors;

import static org.assertj.core.api.Assertions.assertThat;

class CrawlerServiceTest {

    private static class StubPageFetcher implements PageFetcher {
        private final Map<String, PageResult> responses;
        final List<String> requested = new java.util.ArrayList<>();

        StubPageFetcher(Map<String, PageResult> responses) {
            this.responses = responses;
        }

        @Override
        public PageResult fetch(String url) {
            requested.add(url);
            PageResult result = responses.get(url);
            if (result != null) {
                return result;
            }
            return PageResult.success(200, "");
        }
    }

    private static PageResult page(String html) {
        return PageResult.success(200, html);
    }

    private static PageResult httpError(int statusCode) {
        return PageResult.failure(FetchStatus.HTTP_ERROR, statusCode);
    }

    private static PageResult timeout() {
        return PageResult.failure(FetchStatus.TIMEOUT, -1);
    }

    private static Map<String, PageResult> site(Map<String, String> pages) {
        Map<String, PageResult> responses = new HashMap<>();
        pages.forEach((path, html) -> responses.put("https://test.local" + normalizedPath(path), page(html)));
        return responses;
    }

    private static String normalizedPath(String path) {
        String lastSegment = path.substring(path.lastIndexOf('/') + 1);
        boolean hasExtension = lastSegment.contains(".");
        return !path.endsWith("/") && !hasExtension ? path + "/" : path;
    }

    private static String a(String href) {
        return "<a href=\"" + href + "\">link</a>";
    }

    @Test
    void crawlsSameOriginUrls() {
        StubPageFetcher fetcher = new StubPageFetcher(site(Map.of(
                "/", a("/login") + a("/products"),
                "/login/", "empty",
                "/products/", "empty"
        )));
        CrawlerService crawler = new CrawlerService(fetcher);

        CrawlerService.CrawlResult result = crawler.crawl("https://test.local");

        assertThat(result.status()).isEqualTo(CrawlerService.CrawlStatus.COMPLETED);
        List<String> urls = result.discoveredUrls().stream()
                .map(CrawlerService.DiscoveredUrl::url)
                .collect(Collectors.toList());
        assertThat(urls).containsExactlyInAnyOrder(
                "https://test.local/",
                "https://test.local/login/",
                "https://test.local/products/");
    }

    @Test
    void neverCrawlsExternalDomains() {
        StubPageFetcher fetcher = new StubPageFetcher(site(Map.of(
                "/", a("https://google.com/") + a("https://github.com/x") + a("https://cdn.test.local/a")
                        + a("//evil.local/x") + a("/internal")
        )));
        CrawlerService crawler = new CrawlerService(fetcher);

        CrawlerService.CrawlResult result = crawler.crawl("https://test.local");

        assertThat(result.discoveredUrls())
                .extracting(CrawlerService.DiscoveredUrl::url)
                .containsExactly("https://test.local/", "https://test.local/internal/");
        assertThat(fetcher.requested).allSatisfy(url ->
                assertThat(url).startsWith("https://test.local"));
    }

    @Test
    void resolvesRelativeUrlsAgainstCurrentPage() {
        StubPageFetcher fetcher = new StubPageFetcher(site(Map.of(
                "/docs/", a("/about") + a("guide") + a("../users") + a("https://test.local/contact")
        )));
        CrawlerService crawler = new CrawlerService(fetcher);

        CrawlerService.CrawlResult result = crawler.crawl("https://test.local/docs");

        assertThat(result.discoveredUrls())
                .extracting(CrawlerService.DiscoveredUrl::url)
                .containsExactlyInAnyOrder(
                        "https://test.local/docs/",
                        "https://test.local/about/",
                        "https://test.local/docs/guide/",
                        "https://test.local/users/",
                        "https://test.local/contact/");
    }

    @Test
    void removesFragments() {
        StubPageFetcher fetcher = new StubPageFetcher(site(Map.of(
                "/", a("/about#team") + a("/about") + a("/#top")
        )));
        CrawlerService crawler = new CrawlerService(fetcher);

        CrawlerService.CrawlResult result = crawler.crawl("https://test.local");

        assertThat(result.discoveredUrls())
                .extracting(CrawlerService.DiscoveredUrl::url)
                .containsExactly("https://test.local/", "https://test.local/about/");
    }

    @Test
    void preservesQueryParametersInDiscoveredUrl() {
        StubPageFetcher fetcher = new StubPageFetcher(site(Map.of(
                "/", a("/products?page=1") + a("/products?page=2")
        )));
        CrawlerService crawler = new CrawlerService(fetcher);

        CrawlerService.CrawlResult result = crawler.crawl("https://test.local");

        assertThat(result.discoveredUrls())
                .extracting(CrawlerService.DiscoveredUrl::url)
                .containsExactlyInAnyOrder(
                        "https://test.local/",
                        "https://test.local/products/?page=1",
                        "https://test.local/products/?page=2");
    }

    @Test
    void doesNotCrawlTheSameUrlTwice() {
        StubPageFetcher fetcher = new StubPageFetcher(site(Map.of(
                "/", a("/a") + a("/b"),
                "/a/", a("/b") + a("/"),
                "/b/", a("/a")
        )));
        CrawlerService crawler = new CrawlerService(fetcher);

        CrawlerService.CrawlResult result = crawler.crawl("https://test.local");

        assertThat(result.discoveredUrls()).hasSize(3);
        assertThat(fetcher.requested).hasSize(3);
    }

    @Test
    void respectsMaximumDepth() {
        StubPageFetcher fetcher = new StubPageFetcher(site(Map.of(
                "/", a("/l1"),
                "/l1", a("/l2"),
                "/l2", a("/l3"),
                "/l3", a("/l4"),
                "/l4", a("/l5")
        )));
        CrawlerService crawler = new CrawlerService(fetcher);

        CrawlerService.CrawlResult result = crawler.crawl("https://test.local");

        assertThat(result.discoveredUrls())
                .extracting(CrawlerService.DiscoveredUrl::url)
                .containsExactlyInAnyOrder(
                        "https://test.local/",
                        "https://test.local/l1/",
                        "https://test.local/l2/",
                        "https://test.local/l3/");
    }

    @Test
    void respectsMaximumPageCount() {
        Map<String, String> pages = new HashMap<>();
        StringBuilder children = new StringBuilder();
        for (int i = 0; i < 60; i++) {
            children.append(a("/p" + i));
            pages.put("/p" + i, "");
        }
        pages.put("/", children.toString());
        StubPageFetcher fetcher = new StubPageFetcher(site(pages));
        CrawlerService crawler = new CrawlerService(fetcher);

        CrawlerService.CrawlResult result = crawler.crawl("https://test.local");

        assertThat(result.status()).isEqualTo(CrawlerService.CrawlStatus.MAX_PAGES_REACHED);
        assertThat(result.pagesVisited()).isEqualTo(CrawlerService.MAX_PAGES);
        assertThat(result.discoveredUrls()).hasSize(CrawlerService.MAX_PAGES);
        assertThat(fetcher.requested).hasSize(CrawlerService.MAX_PAGES);
    }

    @Test
    void skipsNonPageResources() {
        StubPageFetcher fetcher = new StubPageFetcher(site(Map.of(
                "/", a("/style.css") + a("/logo.png") + a("/report.pdf") + a("/video.mp4")
                        + a("/icon.ico") + a("/real-page")
        )));
        CrawlerService crawler = new CrawlerService(fetcher);

        CrawlerService.CrawlResult result = crawler.crawl("https://test.local");

        assertThat(result.discoveredUrls())
                .extracting(CrawlerService.DiscoveredUrl::url)
                .containsExactly("https://test.local/", "https://test.local/real-page/");
    }

    @Test
    void continuesAfterFailedPages() {
        StubPageFetcher fetcher = new StubPageFetcher(site(Map.of(
                "/", a("/ok1") + a("/timeout") + a("/notfound") + a("/ok2")
        )));
        fetcher.responses.put("https://test.local/timeout/", timeout());
        fetcher.responses.put("https://test.local/notfound/", httpError(404));
        CrawlerService crawler = new CrawlerService(fetcher);

        CrawlerService.CrawlResult result = crawler.crawl("https://test.local");

        assertThat(result.discoveredUrls())
                .extracting(CrawlerService.DiscoveredUrl::url)
                .containsExactlyInAnyOrder(
                        "https://test.local/",
                        "https://test.local/ok1/",
                        "https://test.local/ok2/");
        assertThat(result.failedRequests()).isEqualTo(2);
        assertThat(result.status()).isEqualTo(CrawlerService.CrawlStatus.COMPLETED);
    }

    @Test
    void reportsUnreachableStartUrl() {
        StubPageFetcher fetcher = new StubPageFetcher(new HashMap<>());
        fetcher.responses.put("https://test.local/", timeout());
        CrawlerService crawler = new CrawlerService(fetcher);

        CrawlerService.CrawlResult result = crawler.crawl("https://test.local");

        assertThat(result.status()).isEqualTo(CrawlerService.CrawlStatus.START_URL_UNREACHABLE);
        assertThat(result.discoveredUrls()).isEmpty();
        assertThat(result.failedRequests()).isEqualTo(1);
    }

    @Test
    void failsFastOnMalformedBaseUrl() {
        CrawlerService crawler = new CrawlerService(new StubPageFetcher(new HashMap<>()));

        CrawlerService.CrawlResult result = crawler.crawl("not-a-url");

        assertThat(result.status()).isEqualTo(CrawlerService.CrawlStatus.FAILED);
        assertThat(result.pagesVisited()).isZero();
    }
}
