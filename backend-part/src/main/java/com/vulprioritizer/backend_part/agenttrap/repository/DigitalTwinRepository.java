package com.vulprioritizer.backend_part.agenttrap.repository;

import com.vulprioritizer.backend_part.agenttrap.entity.DigitalTwin;
import com.vulprioritizer.backend_part.targets.entity.Target;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface DigitalTwinRepository extends JpaRepository<DigitalTwin, Long> {

    Optional<DigitalTwin> findByTwinKey(String twinKey);

    Optional<DigitalTwin> findByTarget(Target target);
}
