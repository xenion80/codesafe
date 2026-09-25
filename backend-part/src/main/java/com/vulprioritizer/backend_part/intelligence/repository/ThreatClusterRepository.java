package com.vulprioritizer.backend_part.intelligence.repository;

import com.vulprioritizer.backend_part.intelligence.entity.ThreatCluster;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface ThreatClusterRepository extends JpaRepository<ThreatCluster, Long> {

    Optional<ThreatCluster> findByClusterKey(String clusterKey);
}
