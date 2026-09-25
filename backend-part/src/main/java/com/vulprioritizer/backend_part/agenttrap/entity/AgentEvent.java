package com.vulprioritizer.backend_part.agenttrap.entity;

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
 * Ordered observable request event inside a session.
 * Metadata only: never credentials, cookies, or auth headers.
 * Schema: agent_event (V1__cyber_total_foundation.sql).
 */
@Entity
@Table(name = "agent_event")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AgentEvent {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "agent_session_id", nullable = false)
    private AgentSession agentSession;

    /** Denormalized for fast queries; always the session's target. */
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "target_id", nullable = false)
    private com.vulprioritizer.backend_part.targets.entity.Target target;

    @Column(name = "sequence_number", nullable = false)
    private Integer sequenceNumber;

    @Column(name = "occurred_at", nullable = false)
    private LocalDateTime occurredAt;

    @Column(name = "http_method", nullable = false, length = 16)
    private String httpMethod;

    /** Requested path only — no query string secrets. */
    @Column(nullable = false, length = 512)
    private String path;

    @Column(name = "response_status")
    private Integer responseStatus;

    @Column(name = "classification_signal", length = 64)
    private String classificationSignal;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 32)
    private RoutingDestination routing;

    /** Sanitized request metadata only. */
    @JdbcTypeCode(SqlTypes.JSON)
    @Column(columnDefinition = "jsonb")
    private Map<String, Object> metadata;
}
