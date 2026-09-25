package com.vulprioritizer.backend_part.intelligence.repository;

import com.vulprioritizer.backend_part.intelligence.entity.ThreatScore;
import com.vulprioritizer.backend_part.agenttrap.entity.AgentSession;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface ThreatScoreRepository extends JpaRepository<ThreatScore, Long> {

    Optional<ThreatScore> findFirstByAgentSessionOrderByScoredAtDesc(AgentSession agentSession);
}
