package com.vulprioritizer.backend_part.alert.entity;

import com.vulprioritizer.backend_part.agenttrap.entity.AgentSession;
import com.vulprioritizer.backend_part.sentinel.entity.SecurityFinding;
import com.vulprioritizer.backend_part.sentinel.entity.Severity;
import com.vulprioritizer.backend_part.targets.entity.Target;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

@Entity
@Table(name = "security_alert")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class SecurityAlert {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "target_id", nullable = false)
    private Target target;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "agent_session_id")
    private AgentSession agentSession;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "security_finding_id")
    private SecurityFinding securityFinding;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 32)
    private Severity severity;

    @Column(nullable = false)
    private String title;

    @Column
    private String message;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 32)
    private AlertStatus status;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    @Column(name = "resolved_at")
    private LocalDateTime resolvedAt;
}
