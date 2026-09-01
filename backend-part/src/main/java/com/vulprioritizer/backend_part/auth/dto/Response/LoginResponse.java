package com.vulprioritizer.backend_part.auth.dto.Response;

import com.vulprioritizer.backend_part.user.entity.Role;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class LoginResponse {
    private Long id;
    private String email;
    private Role role;
    private String refreshToken;
    private String accessToken;
}
