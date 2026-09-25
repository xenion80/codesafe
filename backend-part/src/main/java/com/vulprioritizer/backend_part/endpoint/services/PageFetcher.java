package com.vulprioritizer.backend_part.endpoint.services;

/**
 * Abstraction over the HTTP layer used by the crawler.
 *
 * <p>Keeping fetching behind an interface lets {@link CrawlerService} stay focused on
 * traversal logic and makes the whole crawl unit-testable with stubbed responses.</p>
 */
public interface PageFetcher {

    /** Why a fetch failed, as precisely as practical without crashing the crawl. */
    enum FetchStatus {
        /** Page fetched successfully (2xx). */
        SUCCESS,
        /** The server answered with a non-2xx status (e.g. 404, 500). */
        HTTP_ERROR,
        /** Connection or read timeout. */
        TIMEOUT,
        /** Connection could not be established or broke mid-request. */
        CONNECTION_ERROR,
        /** The URL is malformed or uses an unsupported scheme. */
        INVALID_URL
    }

    /**
     * Outcome of a single GET request.
     *
     * @param status     fetch outcome
     * @param statusCode HTTP status code, or -1 when the request never completed
     * @param body       response body (only present on {@link FetchStatus#SUCCESS})
     */
    record PageResult(FetchStatus status, int statusCode, String body) {

        public static PageResult success(int statusCode, String body) {
            return new PageResult(FetchStatus.SUCCESS, statusCode, body);
        }

        public static PageResult failure(FetchStatus status, int statusCode) {
            return new PageResult(status, statusCode, null);
        }

        public boolean isSuccess() {
            return status == FetchStatus.SUCCESS;
        }
    }

    /**
     * Performs a GET request against {@code url}.
     *
     * <p>Implementations must never throw for ordinary failures; every failure mode is
     * reported as a {@link PageResult} so one bad page cannot stop a crawl.</p>
     */
    PageResult fetch(String url);
}
