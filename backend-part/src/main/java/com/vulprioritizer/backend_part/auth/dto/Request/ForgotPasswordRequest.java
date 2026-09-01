package com.vulprioritizer.backend_part.auth.dto.Request;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class ForgotPasswordRequest {

    @Email(message = "enter correct email")
    @NotBlank(message = "the email section cannot be blank")
    private String email;
}
