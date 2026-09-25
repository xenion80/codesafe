package com.vulprioritizer.backend_part.agenttrap.repository;

import com.vulprioritizer.backend_part.agenttrap.entity.AgentClassification;
import com.vulprioritizer.backend_part.agenttrap.entity.AgentSession;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface AgentClassificationRepository extends JpaRepository<AgentClassification, Long> {

    List<AgentClassification> findByAgentSessionOrderByClassifiedAtDesc(AgentSession agentSession);
}
