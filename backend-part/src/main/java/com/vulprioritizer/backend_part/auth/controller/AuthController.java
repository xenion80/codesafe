package com.vulprioritizer.backend_part.auth.controller;

import com.vulprioritizer.backend_part.auth.dto.Request.ForgotPasswordRequest;
import com.vulprioritizer.backend_part.auth.dto.Request.LoginRequest;
import com.vulprioritizer.backend_part.auth.dto.Request.ResetPasswordRequest;
import com.vulprioritizer.backend_part.auth.dto.Response.LoginResponse;
import com.vulprioritizer.backend_part.auth.services.AuthService;
import com.vulprioritizer.backend_part.common.response.ApiResponse;
import com.vulprioritizer.backend_part.user.dto.request.SignUpInputModel;
import com.vulprioritizer.backend_part.user.dto.response.UserResponse;
import com.vulprioritizer.backend_part.user.service.UserService;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationServiceException;
import org.springframework.web.bind.annotation.*;

import java.util.Arrays;

@RestController
@RequiredArgsConstructor
@RequestMapping("/auth")
public class AuthController {
    private final UserService userService;
    private final AuthService authService;

    @PostMapping("/register")
    public ResponseEntity<ApiResponse<UserResponse>> signUp(@Valid @RequestBody SignUpInputModel signUpInputModel){
        UserResponse response=userService.signUp(signUpInputModel);
        return ResponseEntity.ok(
                ApiResponse.success("User Register successfully",response)

        );

    }
    @PostMapping("/login")
    public ResponseEntity<ApiResponse<LoginResponse>> login(@Valid@RequestBody LoginRequest loginRequest, HttpServletRequest request, HttpServletResponse response){
        LoginResponse loginResponse=authService.login(loginRequest);
        Cookie cookie=new Cookie("refreshToken",loginResponse.getRefreshToken());
        cookie.setHttpOnly(true);
        cookie.setSecure(true);
        cookie.setAttribute("SameSite", "Strict");
        cookie.setPath("/auth");
        response.addCookie(cookie);
        return ResponseEntity.ok(ApiResponse.success("Logged in successfully",loginResponse));

    }
    @PostMapping("/logout")
    public ResponseEntity<ApiResponse<String>> logout(HttpServletRequest request,HttpServletResponse response){
        authService.logout(request,response);
        return ResponseEntity.ok(
                ApiResponse.success("Logged out Successfully",null)
        );
    }

    @PostMapping("/refresh")
    public ResponseEntity<ApiResponse<LoginResponse>> refresh(HttpServletRequest request){
        Cookie[] cookies = request.getCookies();
        if (cookies == null) {
            throw new AuthenticationServiceException("Refresh token not found in request");
        }
        String token = Arrays.stream(cookies)
                .filter(cookie -> "refreshToken".equals(cookie.getName()))
                .findFirst()
                .map(Cookie::getValue)
                .orElseThrow(()->new AuthenticationServiceException("Refreshtoken is not found in the request"));
        LoginResponse response=authService.refresh(token);
        return ResponseEntity.ok(
                ApiResponse.success("Refreshed accessed token successfully",response)
        );
    }
    @PostMapping("/forgot-password")
    public ResponseEntity<ApiResponse<String>> forgot_password(@Valid @RequestBody ForgotPasswordRequest request){
        String resetToken = authService.requestPasswordReset(request.getEmail());
        if (resetToken == null) {
            // Don't reveal whether the account exists
            return ResponseEntity.ok(
                    ApiResponse.success("If an account with this email exists, a reset token has been generated. Check the response data.", null)
            );
        }
        return ResponseEntity.ok(
                ApiResponse.success("Reset token generated. Copy the token from 'data' and use it with /auth/reset-password.", resetToken)
        );
    }

    @PostMapping("/reset-password")
    public ResponseEntity<ApiResponse<String>> reset_Password(@Valid@RequestBody ResetPasswordRequest request){
        authService.resetPassword(request.getToken(),request.getNewPassword());
        return ResponseEntity.ok(
                ApiResponse.success("The password has been reset successfully",null)
        );
    }
}
