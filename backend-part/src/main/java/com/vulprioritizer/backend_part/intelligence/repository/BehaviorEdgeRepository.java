package com.vulprioritizer.backend_part.intelligence.repository;

import com.vulprioritizer.backend_part.intelligence.entity.BehaviorEdge;
import com.vulprioritizer.backend_part.agenttrap.entity.AgentSession;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface BehaviorEdgeRepository extends JpaRepository<BehaviorEdge, Long> {

    List<BehaviorEdge> findByAgentSession(AgentSession agentSession);
}
