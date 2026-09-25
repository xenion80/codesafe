package com.vulprioritizer.backend_part.intelligence.entity;

import com.vulprioritizer.backend_part.agenttrap.entity.AgentSession;
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
 * Explainable risk score computed for a session.
 * Schema: threat_score (V1__cyber_total_foundation.sql).
 */
@Entity
@Table(name = "threat_score")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ThreatScore {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "agent_session_id", nullable = false)
    private AgentSession agentSession;

    /** 0-100. */
    @Column(nullable = false)
    private Integer score;

    /** e.g. BENIGN | LOW | ELEVATED | HIGH | CRITICAL. */
    @Column(nullable = false, length = 32)
    private String category;

    @Column(nullable = false)
    private Double confidence;

    /** Per-signal contribution — the explainability payload. */
    @JdbcTypeCode(SqlTypes.JSON)
    @Column(nullable = false, columnDefinition = "jsonb")
    private Map<String, Object> breakdown;

    /** Seen before in the network? */
    @Column(name = "known_behavior", nullable = false)
    private Boolean knownBehavior;

    /** e.g. MONITOR | REDIRECT_TO_TWIN | BLOCK. */
    @Column(name = "recommended_action", length = 64)
    private String recommendedAction;

    @Column(name = "scored_at", nullable = false)
    private LocalDateTime scoredAt;

    @Column(name = "scorer_version", nullable = false, length = 32)
    private String scorerVersion;
}
