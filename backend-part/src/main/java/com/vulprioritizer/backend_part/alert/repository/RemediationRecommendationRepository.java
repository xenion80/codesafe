package com.vulprioritizer.backend_part.alert.repository;

import com.vulprioritizer.backend_part.alert.entity.RemediationRecommendation;
import com.vulprioritizer.backend_part.sentinel.entity.SecurityFinding;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface RemediationRecommendationRepository extends JpaRepository<RemediationRecommendation, Long> {

    List<RemediationRecommendation> findBySecurityFindingOrderByPriorityDesc(SecurityFinding securityFinding);
}
