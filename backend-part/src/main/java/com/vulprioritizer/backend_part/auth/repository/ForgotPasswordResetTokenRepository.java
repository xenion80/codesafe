package com.vulprioritizer.backend_part.auth.repository;

import com.vulprioritizer.backend_part.auth.entity.ForgotPasswordResetToken;
import com.vulprioritizer.backend_part.user.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface ForgotPasswordResetTokenRepository extends JpaRepository<ForgotPasswordResetToken, Long> {
    Optional<ForgotPasswordResetToken> findByToken(String token);

    void deleteByUser(User user);
}
