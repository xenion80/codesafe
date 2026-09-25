package com.vulprioritizer.backend_part.sentinel.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.LocalDateTime;
import java.util.Map;

/**
 * Aggregate result captured for each completed scan run.
 * Schema: scan_result (V1__cyber_total_foundation.sql).
 */
@Entity
@Table(name = "scan_result")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ScanResult {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @OneToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "scan_id", nullable = false, unique = true)
    private SentinelScan scan;

    @Column(name = "endpoints_checked", nullable = false)
    private Integer endpointsChecked;

    @Column(name = "findings_count", nullable = false)
    private Integer findingsCount;

    @Column(name = "average_response_ms")
    private Integer averageResponseMs;

    @Column(name = "checked_at", nullable = false)
    private LocalDateTime checkedAt;

    /** Structured per-check rollup. */
    @JdbcTypeCode(SqlTypes.JSON)
    @Column(columnDefinition = "jsonb")
    private Map<String, Object> details;
}
