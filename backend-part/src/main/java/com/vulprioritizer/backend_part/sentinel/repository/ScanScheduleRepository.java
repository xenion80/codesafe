package com.vulprioritizer.backend_part.sentinel.repository;

import com.vulprioritizer.backend_part.sentinel.entity.ScanSchedule;
import com.vulprioritizer.backend_part.targets.entity.Target;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ScanScheduleRepository extends JpaRepository<ScanSchedule, Long> {

    List<ScanSchedule> findByTarget(Target target);

    List<ScanSchedule> findByEnabledTrue();
}
