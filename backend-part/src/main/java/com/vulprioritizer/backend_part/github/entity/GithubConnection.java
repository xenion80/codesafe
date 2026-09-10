package com.vulprioritizer.backend_part.github.entity;

import com.vulprioritizer.backend_part.user.entity.User;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

@Entity
@NoArgsConstructor
@AllArgsConstructor
@Getter
@Setter
public class GithubConnection {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @OneToOne
    private User user;

    private String githubUserId;

    private String githubUsername;

    @Column(nullable = false)
    private String accessToken;

    private LocalDateTime expiresAt;

    private LocalDateTime connectedAt;
}
