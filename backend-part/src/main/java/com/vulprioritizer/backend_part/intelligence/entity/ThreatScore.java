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

    @Column(nullable = false)
    private Integer score;

    @Column(nullable = false, length = 32)
    private String category;

    @Column(nullable = false)
    private Double confidence;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(nullable = false, columnDefinition = "jsonb")
    private Map<String, Object> breakdown;

    @Column(name = "known_behavior", nullable = false)
    private Boolean knownBehavior;

    @Column(name = "recommended_action", length = 64)
    private String recommendedAction;

    @Column(name = "scored_at", nullable = false)
    private LocalDateTime scoredAt;

    @Column(name = "scorer_version", nullable = false, length = 32)
    private String scorerVersion;
}
