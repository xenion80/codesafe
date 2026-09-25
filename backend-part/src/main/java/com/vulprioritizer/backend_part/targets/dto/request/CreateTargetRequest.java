package com.vulprioritizer.backend_part.targets.dto.request;

import com.vulprioritizer.backend_part.targets.entity.TargetType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class CreateTargetRequest {
    @NotBlank(message = "Target name is required")
    private String name;

    @NotNull(message = "Target type is required")
    private TargetType type;

    @NotBlank(message = "Base URL is required")
    private String baseUrl;

    @NotBlank(message = "Target description is required")
    private String description;
}
