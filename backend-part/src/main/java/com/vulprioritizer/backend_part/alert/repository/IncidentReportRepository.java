package com.vulprioritizer.backend_part.alert.repository;

import com.vulprioritizer.backend_part.alert.entity.IncidentReport;
import com.vulprioritizer.backend_part.targets.entity.Target;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface IncidentReportRepository extends JpaRepository<IncidentReport, Long> {

    Page<IncidentReport> findByTargetOrderByCreatedAtDesc(Target target, Pageable pageable);
}
