package com.vulprioritizer.backend_part.endpoint.repository;

import com.vulprioritizer.backend_part.endpoint.entity.Endpoint;
import com.vulprioritizer.backend_part.endpoint.entity.MethodType;
import com.vulprioritizer.backend_part.targets.entity.Target;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
@Repository
public interface EndpointRepository extends JpaRepository<Endpoint, Long> {
    Optional<List<Endpoint>> findByTarget(Target target);
    Optional<Endpoint> findByTargetAndPathAndMethod(
            Target target,
            String path,
            MethodType method
    );
}