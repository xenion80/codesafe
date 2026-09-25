package com.vulprioritizer.backend_part.targets.repository;

import com.vulprioritizer.backend_part.project.entity.Project;
import com.vulprioritizer.backend_part.targets.dto.response.TargetResponse;
import com.vulprioritizer.backend_part.targets.entity.Target;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface TargetRepository extends JpaRepository<Target, Long> {
    Page<TargetResponse> findByProjectAndDeletedFalse(Pageable pageable, Project project);
    Optional<Target> findByIdAndDeletedFalse(Long targetId);
}