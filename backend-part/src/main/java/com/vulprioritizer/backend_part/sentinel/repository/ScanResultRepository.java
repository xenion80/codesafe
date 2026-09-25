package com.vulprioritizer.backend_part.sentinel.repository;

import com.vulprioritizer.backend_part.sentinel.entity.ScanResult;
import com.vulprioritizer.backend_part.sentinel.entity.SentinelScan;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface ScanResultRepository extends JpaRepository<ScanResult, Long> {

    Optional<ScanResult> findByScan(SentinelScan scan);
}
