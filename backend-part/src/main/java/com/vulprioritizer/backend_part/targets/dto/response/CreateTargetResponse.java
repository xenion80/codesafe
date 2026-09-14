package com.vulprioritizer.backend_part.targets.dto.response;

import com.vulprioritizer.backend_part.targets.entity.TargetType;
import lombok.Data;

import java.time.LocalDateTime;

@Data
public class CreateTargetResponse {

    private Long id;

    private String name;

    private TargetType type;

    private String baseUrl;

    private String description;

    private LocalDateTime createdAt;

    private Long projectId;
}