package com.vulprioritizer.backend_part.targets.entity;

import com.vulprioritizer.backend_part.project.entity.Project;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Target {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String name;

    @Column(nullable = false)
    private String description;

    @Column(nullable = false)
    @Enumerated(EnumType.STRING)
    private TargetType type;

    @Column(nullable = false)
    private String url;

    @ManyToOne(fetch=FetchType.EAGER)
    @JoinColumn(name = "Project_id",nullable = false)
    private Project project;

    @Column(nullable = false)
    private LocalDateTime created_at;

    @Column(nullable = false)
    private LocalDateTime updated_at;

    private Boolean deleted=false;




}
