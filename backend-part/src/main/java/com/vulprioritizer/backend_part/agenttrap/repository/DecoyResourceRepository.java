package com.vulprioritizer.backend_part.agenttrap.repository;

import com.vulprioritizer.backend_part.agenttrap.entity.DecoyResource;
import com.vulprioritizer.backend_part.agenttrap.entity.DigitalTwin;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface DecoyResourceRepository extends JpaRepository<DecoyResource, Long> {

    List<DecoyResource> findByDigitalTwin(DigitalTwin digitalTwin);
}
