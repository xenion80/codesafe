package com.vulprioritizer.backend_part.intelligence.entity;

import com.vulprioritizer.backend_part.agenttrap.entity.AgentSession;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

@Entity
@Table(name = "behavior_node")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class BehaviorNode {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "agent_session_id", nullable = false)
    private AgentSession agentSession;

    @Column(name = "node_key", nullable = false, length = 128)
    private String nodeKey;

    @Column(name = "node_type", nullable = false, length = 32)
    private String nodeType;

    @Column
    private String label;

    @Column(name = "risk_contribution", nullable = false)
    private Double riskContribution;

    @Column(name = "first_seen_at", nullable = false)
    private LocalDateTime firstSeenAt;

    @Column(name = "last_seen_at", nullable = false)
    private LocalDateTime lastSeenAt;
}
