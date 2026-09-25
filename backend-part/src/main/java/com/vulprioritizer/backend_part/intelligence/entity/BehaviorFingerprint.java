package com.vulprioritizer.backend_part.intelligence.entity;

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
 * Anonymized behavioral fingerprint shared across the intelligence network.
 * No customer URLs, credentials, or personal data.
 * Schema: behavior_fingerprint (V1__cyber_total_foundation.sql).
 */
@Entity
@Table(name = "behavior_fingerprint")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class BehaviorFingerprint {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** Deterministic hash of the normalized behavior sequence. */
    @Column(name = "fingerprint_hash", nullable = false, unique = true, length = 64)
    private String fingerprintHash;

    /** Normalized behavior sequence (no sensitive data). */
    @JdbcTypeCode(SqlTypes.JSON)
    @Column(nullable = false, columnDefinition = "jsonb")
    private Map<String, Object> sequence;

    /** Observable signals summary. */
    @JdbcTypeCode(SqlTypes.JSON)
    @Column(columnDefinition = "jsonb")
    private Map<String, Object> signals;

    @Column(name = "first_seen_at", nullable = false)
    private LocalDateTime firstSeenAt;

    @Column(name = "last_seen_at", nullable = false)
    private LocalDateTime lastSeenAt;

    /** Distinct protected sites reporting this fingerprint. */
    @Column(name = "reporting_sites", nullable = false)
    private Integer reportingSites;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;
}
