package com.vulprioritizer.backend_part.intelligence.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

/**
 * Membership of a fingerprint in a threat cluster.
 * Schema: threat_cluster_member (V1__cyber_total_foundation.sql).
 */
@Entity
@Table(name = "threat_cluster_member")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ThreatClusterMember {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "threat_cluster_id", nullable = false)
    private ThreatCluster threatCluster;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "fingerprint_id", nullable = false)
    private BehaviorFingerprint fingerprint;

    @Column(name = "joined_at", nullable = false)
    private LocalDateTime joinedAt;
}
