package com.vulprioritizer.backend_part.intelligence.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

/**
 * Threat cluster grouping similar fingerprints.
 * Schema: threat_cluster (V1__cyber_total_foundation.sql).
 */
@Entity
@Table(name = "threat_cluster")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ThreatCluster {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "cluster_key", nullable = false, unique = true, length = 64)
    private String clusterKey;

    @Column
    private String label;

    @Column(name = "member_count", nullable = false)
    private Integer memberCount;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;
}
