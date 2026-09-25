package com.vulprioritizer.backend_part.sentinel.repository;

import com.vulprioritizer.backend_part.sentinel.entity.SecurityFinding;
import com.vulprioritizer.backend_part.sentinel.entity.SentinelScan;
import com.vulprioritizer.backend_part.targets.entity.Target;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface SecurityFindingRepository extends JpaRepository<SecurityFinding, Long> {

    Page<SecurityFinding> findByScan(SentinelScan scan, Pageable pageable);

    Page<SecurityFinding> findByTargetAndActiveTrue(Target target, Pageable pageable);
}
