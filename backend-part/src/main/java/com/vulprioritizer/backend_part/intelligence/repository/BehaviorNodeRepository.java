package com.vulprioritizer.backend_part.intelligence.repository;

import com.vulprioritizer.backend_part.intelligence.entity.BehaviorNode;
import com.vulprioritizer.backend_part.agenttrap.entity.AgentSession;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface BehaviorNodeRepository extends JpaRepository<BehaviorNode, Long> {

    List<BehaviorNode> findByAgentSession(AgentSession agentSession);
}
