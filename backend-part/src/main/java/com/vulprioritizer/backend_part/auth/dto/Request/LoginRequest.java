package com.vulprioritizer.backend_part.auth.dto.Request;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class LoginRequest {
    @Email(message = "invalid email format")
    @NotBlank(message = "email cannot be blank")
    private String email;
    @NotBlank(message = "password cannot be empty")
    private String password;
}
