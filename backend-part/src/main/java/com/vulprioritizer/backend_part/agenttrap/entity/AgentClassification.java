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

@Entity
@Table(name = "agent_classification")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AgentClassification {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "agent_session_id", nullable = false)
    private AgentSession agentSession;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 32)
    private ClassificationCategory category;

    @Column(nullable = false)
    private Double confidence;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(columnDefinition = "jsonb")
    private Map<String, Object> signals;

    @Column(name = "classified_at", nullable = false)
    private LocalDateTime classifiedAt;

    @Column(name = "classifier_version", nullable = false, length = 32)
    private String classifierVersion;
}
