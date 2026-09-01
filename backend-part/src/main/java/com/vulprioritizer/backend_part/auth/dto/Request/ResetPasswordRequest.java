package com.vulprioritizer.backend_part.auth.dto.Request;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class ResetPasswordRequest {
    @NotBlank(message = "Token is required")
    private String token;

    @NotBlank(message = "password is necessary")
    private String newPassword;
}
