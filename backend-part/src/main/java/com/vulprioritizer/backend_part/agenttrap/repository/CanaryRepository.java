package com.vulprioritizer.backend_part.agenttrap.repository;

import com.vulprioritizer.backend_part.agenttrap.entity.Canary;
import com.vulprioritizer.backend_part.targets.entity.Target;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface CanaryRepository extends JpaRepository<Canary, Long> {

    List<Canary> findByTarget(Target target);

    Optional<Canary> findByIdentifier(String identifier);
}
