package com.vulprioritizer.backend_part.sentinel.entity;

/**
 * Lifecycle states of a Sentinel scan.
 */
public enum ScanStatus {
    PENDING,
    RUNNING,
    COMPLETED,
    FAILED,
    CANCELLED
}
