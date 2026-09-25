package com.vulprioritizer.backend_part.agenttrap.entity;

import com.vulprioritizer.backend_part.targets.entity.Target;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

/**
 * Canary sensor embedded into pages or twin responses.
 * Schema: canary (V1__cyber_total_foundation.sql).
 */
@Entity
@Table(name = "canary")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Canary {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "target_id", nullable = false)
    private Target target;

    @Enumerated(EnumType.STRING)
    @Column(name = "canary_type", nullable = false, length = 32)
    private CanaryType canaryType;

    /** Unique, non-sensitive marker value. */
    @Column(nullable = false, unique = true, length = 128)
    private String identifier;

    @Column(nullable = false)
    private Boolean active;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    @Column(name = "last_triggered_at")
    private LocalDateTime lastTriggeredAt;

    @Column(name = "trigger_count", nullable = false)
    private Integer triggerCount;
}
