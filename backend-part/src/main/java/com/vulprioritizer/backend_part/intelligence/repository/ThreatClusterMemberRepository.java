package com.vulprioritizer.backend_part.intelligence.repository;

import com.vulprioritizer.backend_part.intelligence.entity.ThreatClusterMember;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ThreatClusterMemberRepository extends JpaRepository<ThreatClusterMember, Long> {

    List<ThreatClusterMember> findByThreatClusterId(Long threatClusterId);
}
