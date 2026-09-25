package com.vulprioritizer.backend_part.intelligence.repository;

import com.vulprioritizer.backend_part.intelligence.entity.BehaviorFingerprint;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface BehaviorFingerprintRepository extends JpaRepository<BehaviorFingerprint, Long> {

    Optional<BehaviorFingerprint> findByFingerprintHash(String fingerprintHash);
}
