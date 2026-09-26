package com.vulprioritizer.backend_part.endpoint.dto.response;

import lombok.Data;

@Data
public class DiscoveryResponse {

    private int pagesVisited;

    private int endpointsDiscovered;

    private int newEndpoints;

    private long durationMs;

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
