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
 * Isolated synthetic environment per target (Agent Trap Digital Twin).
 * The twin never touches production data; responses are synthetic only.
 * Schema: digital_twin (V1__cyber_total_foundation.sql).
 */
@Entity
@Table(name = "digital_twin")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class DigitalTwin {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "target_id", nullable = false, unique = true)
    private Target target;

    /** Opaque routing key used by the local demo routing layer. */
    @Column(name = "twin_key", nullable = false, unique = true, length = 64)
    private String twinKey;

    /** Local synthetic mount path. */
    @Column(name = "base_path", nullable = false)
    private String basePath;

    @Column(nullable = false)
    private Boolean active;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;
}
