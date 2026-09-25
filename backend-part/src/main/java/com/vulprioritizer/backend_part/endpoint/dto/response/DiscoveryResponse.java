package com.vulprioritizer.backend_part.endpoint.dto.response;

import lombok.Data;

/**
 * Summary of one endpoint-discovery run for a target, returned by
 * {@code POST /targets/{targetId}/discover}. Internal crawler details (queue, depths,
 * raw failures) are deliberately not exposed.
 */
@Data
public class DiscoveryResponse {

    /** Pages the crawler successfully fetched, including the target base URL. */
    private int pagesVisited;

    /** Same-origin page URLs found by the crawl (persistence candidates). */
    private int endpointsDiscovered;

    /** Endpoints newly inserted; re-activated or refreshed rows are not counted. */
    private int newEndpoints;

    /** Wall-clock duration of the crawl in milliseconds. */
    private long durationMs;

    /**
     * Crawl outcome: {@code COMPLETED}, {@code MAX_PAGES_REACHED},
     * {@code MAX_DEPTH_REACHED}, {@code START_URL_UNREACHABLE} or {@code FAILED}.
     */
    private String status;

    public DiscoveryResponse() {
    }

    public DiscoveryResponse(int pagesVisited, int endpointsDiscovered, int newEndpoints, long durationMs, String status) {
        this.pagesVisited = pagesVisited;
        this.endpointsDiscovered = endpointsDiscovered;
        this.newEndpoints = newEndpoints;
        this.durationMs = durationMs;
        this.status = status;
    }
}
