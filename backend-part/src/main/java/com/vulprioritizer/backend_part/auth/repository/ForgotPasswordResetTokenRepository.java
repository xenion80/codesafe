package com.vulprioritizer.backend_part.auth.repository;

import com.vulprioritizer.backend_part.auth.entity.ForgotPasswordResetToken;
import com.vulprioritizer.backend_part.user.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

public interface ForgotPasswordResetTokenRepository extends JpaRepository<ForgotPasswordResetToken, Long> {
    Optional<ForgotPasswordResetToken> findByToken(String token);

    @Modifying
    @Query("DELETE FROM ForgotPasswordResetToken t WHERE t.user = :user")
    void deleteByUser(@Param("user") User user);
}
