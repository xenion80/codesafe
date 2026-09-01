package com.vulprioritizer.backend_part.user.repository;

import com.vulprioritizer.backend_part.user.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface UserRepository extends JpaRepository<User, Long> {
    Optional<User> findByEmail(String email);


}