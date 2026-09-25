package com.vulprioritizer.backend_part.endpoint.dto.response;

import com.vulprioritizer.backend_part.endpoint.entity.MethodType;
import lombok.Data;
import lombok.Value;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * DTO for {@link com.vulprioritizer.backend_part.endpoint.entity.Endpoint}
 */
@Data
public class EndpointResponse {
    Long id;
    String path;
    MethodType method;
    String url;
    LocalDateTime createdAt;
    LocalDateTime updatedAt;
    boolean active;
}