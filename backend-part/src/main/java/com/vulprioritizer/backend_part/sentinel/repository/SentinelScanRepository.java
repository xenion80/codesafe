package com.vulprioritizer.backend_part.sentinel.repository;

import com.vulprioritizer.backend_part.sentinel.entity.ScanStatus;
import com.vulprioritizer.backend_part.sentinel.entity.SentinelScan;
import com.vulprioritizer.backend_part.targets.entity.Target;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface SentinelScanRepository extends JpaRepository<SentinelScan, Long> {

    Page<SentinelScan> findByTargetOrderByCreatedAtDesc(Target target, Pageable pageable);

    List<SentinelScan> findByTargetAndStatus(Target target, ScanStatus status);
}
