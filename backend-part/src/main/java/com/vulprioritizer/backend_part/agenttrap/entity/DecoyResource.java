package com.vulprioritizer.backend_part.agenttrap.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

/**
 * Synthetic decoy resource served by the twin. Content is synthetic only.
 * Schema: decoy_resource (V1__cyber_total_foundation.sql).
 */
@Entity
@Table(name = "decoy_resource")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class DecoyResource {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "digital_twin_id", nullable = false)
    private DigitalTwin digitalTwin;

    @Column(nullable = false, length = 512)
    private String path;

    @Enumerated(EnumType.STRING)
    @Column(name = "resource_type", nullable = false, length = 32)
    private DecoyResourceType resourceType;

    /** Synthetic content only. */
    @Column
    private String content;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;
}
