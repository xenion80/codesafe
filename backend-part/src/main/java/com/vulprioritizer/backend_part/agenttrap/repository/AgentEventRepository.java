package com.vulprioritizer.backend_part.agenttrap.repository;

import com.vulprioritizer.backend_part.agenttrap.entity.AgentEvent;
import com.vulprioritizer.backend_part.agenttrap.entity.AgentSession;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface AgentEventRepository extends JpaRepository<AgentEvent, Long> {

    List<AgentEvent> findByAgentSessionOrderBySequenceNumberAsc(AgentSession agentSession);
}
