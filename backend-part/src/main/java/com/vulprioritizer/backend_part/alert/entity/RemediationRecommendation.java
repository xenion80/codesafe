package com.vulprioritizer.backend_part.alert.entity;

import com.vulprioritizer.backend_part.sentinel.entity.SecurityFinding;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

@Entity
@Table(name = "remediation_recommendation")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class RemediationRecommendation {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "security_finding_id", nullable = false)
    private SecurityFinding securityFinding;

    @Column(nullable = false)
    private String recommendation;

    @Column(nullable = false)
    private Integer priority;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;
}
