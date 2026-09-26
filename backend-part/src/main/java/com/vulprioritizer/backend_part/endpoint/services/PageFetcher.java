package com.vulprioritizer.backend_part.endpoint.services;

public interface PageFetcher {

    enum FetchStatus {
        SUCCESS,
        HTTP_ERROR,
        TIMEOUT,
        CONNECTION_ERROR,
        INVALID_URL
    }

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

    PageResult fetch(String url);
}
