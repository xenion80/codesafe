package com.vulprioritizer.backend_part.github.repository;

import com.vulprioritizer.backend_part.github.entity.GithubConnection;
import com.vulprioritizer.backend_part.user.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface GithubConnectionRepository extends JpaRepository<GithubConnection, Long> {
    Optional<GithubConnection> findByUser(User user);
}
