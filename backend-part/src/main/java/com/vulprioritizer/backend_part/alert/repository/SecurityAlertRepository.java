package com.vulprioritizer.backend_part.alert.repository;

import com.vulprioritizer.backend_part.alert.entity.AlertStatus;
import com.vulprioritizer.backend_part.alert.entity.SecurityAlert;
import com.vulprioritizer.backend_part.targets.entity.Target;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface SecurityAlertRepository extends JpaRepository<SecurityAlert, Long> {

    Page<SecurityAlert> findByTargetOrderByCreatedAtDesc(Target target, Pageable pageable);

    Page<SecurityAlert> findByTargetAndStatusOrderByCreatedAtDesc(Target target, AlertStatus status, Pageable pageable);
}
