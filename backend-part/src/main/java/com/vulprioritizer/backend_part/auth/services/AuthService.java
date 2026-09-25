package com.vulprioritizer.backend_part.auth.services;


import com.vulprioritizer.backend_part.auth.dto.Request.LoginRequest;
import com.vulprioritizer.backend_part.auth.dto.Response.LoginResponse;
import com.vulprioritizer.backend_part.auth.entity.EmailVerificationToken;
import com.vulprioritizer.backend_part.auth.entity.ForgotPasswordResetToken;
import com.vulprioritizer.backend_part.auth.entity.RefreshToken;
import com.vulprioritizer.backend_part.auth.repository.EmailVerificationTokenRepository;
import com.vulprioritizer.backend_part.auth.repository.ForgotPasswordResetTokenRepository;
import com.vulprioritizer.backend_part.auth.repository.RefreshTokenRepository;
import com.vulprioritizer.backend_part.common.exception.InvalidTokenException;
import com.vulprioritizer.backend_part.common.exception.TokenRevokedException;
import com.vulprioritizer.backend_part.common.exception.TokenNotFoundException;

import com.vulprioritizer.backend_part.user.entity.User;
import com.vulprioritizer.backend_part.user.repository.UserRepository;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.transaction.Transactional;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.Optional;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class AuthService {
    private final AuthenticationManager manager;
    private final JwtAuthService jwtAuthService;
    private final PasswordEncoder passwordEncoder;
    private final EmailVerificationTokenRepository emailVerificationTokenRepository;
    private final RefreshTokenRepository refreshTokenRepository;
    private final UserRepository userRepository;
    private final ForgotPasswordResetTokenRepository forgotPasswordResetTokenRepository;
    private final EmailService emailService;

    @Value("${app.base-url}")
    private String baseUrl;

    @Transactional
    public void verify(String token) {
        EmailVerificationToken emailVerificationToken = emailVerificationTokenRepository.findByToken(token)
                .orElseThrow(() -> new TokenNotFoundException("Invalid verification token"));
        if(emailVerificationToken.getExpiresAt().isBefore(LocalDateTime.now())){
            throw new IllegalArgumentException("The token has been expired");
        }
        User user=emailVerificationToken.getUser();
        user.setEmailVerified(true);
        user.setEnabled(true);
        userRepository.save(user);
        emailVerificationTokenRepository.delete(emailVerificationToken);
    }

    public LoginResponse login(@Valid LoginRequest loginRequest) {
        Authentication authentication= manager.authenticate(
                new UsernamePasswordAuthenticationToken(
                        loginRequest.getEmail(),loginRequest.getPassword()
                )
        );
        User user=(User) authentication.getPrincipal();
        String refreshToken= jwtAuthService.generateRefreshToken(user);
        String accessToken= jwtAuthService.generateAccessToken(user);
        RefreshToken token=RefreshToken.builder()
                .token(refreshToken)
                .createdAt(LocalDateTime.now())
                .expiresAt(LocalDateTime.now().plusDays(90))
                .revoked(false)
                .user(user)
                .build();
        refreshTokenRepository.save(token);
        return new LoginResponse(user.getId(),user.getEmail(), user.getRole(),refreshToken,accessToken);
    }

    public void logout(HttpServletRequest request, HttpServletResponse response) {
        String refreshToken = null;
        if (request.getCookies() != null) {
            for (Cookie cookie : request.getCookies()) {
                if ("refreshToken".equals(cookie.getName())) {
                    refreshToken = cookie.getValue();
                    break;
                }
            }
        }
        if(refreshToken!=null){
            RefreshToken token=refreshTokenRepository.findByToken(refreshToken).orElse(null);
            if(token!=null){
                token.setRevoked(true);
                refreshTokenRepository.save(token);
            }
        }
        Cookie cookie=new Cookie("refreshToken","");
        cookie.setHttpOnly(true);
        cookie.setPath("/");
        cookie.setMaxAge(0);
        response.addCookie(cookie);
    }

    public LoginResponse refresh(String token) {
        RefreshToken refreshToken=refreshTokenRepository.findByToken(token).orElseThrow(()->new TokenNotFoundException("RefreshToken not found"));
        if(refreshToken.getRevoked())throw new TokenRevokedException("Refresh token has been revoked");
        if (!jwtAuthService.isTokenValid(token))throw new InvalidTokenException("the token is not valid");
        Long userId= jwtAuthService.extractUserId(token);
        User user =userRepository.findById(userId).orElseThrow(()->new UsernameNotFoundException("Username with this Id not found"));
        String accessToken= jwtAuthService.generateAccessToken(user);
        return new LoginResponse(user.getId(),user.getEmail(),user.getRole(),token,accessToken);

    }

    @Transactional
    public void requestPasswordReset(@Email(message = "enter correct email") @NotBlank(message = "the email section cannot be blank") String email) {
        Optional<User> optionalUser=userRepository.findByEmail(email);
        if(optionalUser.isEmpty())return;
        User user=optionalUser.get();
        String token= UUID.randomUUID().toString();
        ForgotPasswordResetToken passwordResetToken=new ForgotPasswordResetToken();
        passwordResetToken.setToken(token);
        passwordResetToken.setUser(user);
        passwordResetToken.setExpiresAt(LocalDateTime.now().plusMinutes(20));
        forgotPasswordResetTokenRepository.save(passwordResetToken);
        String url=buildResetPasswordUrl(token);
        emailService.sendMail(user.getEmail(),"Reset Password","click here: "+url);
    }
    @Transactional
    public void resetPassword( String token, String newPassword) {
        ForgotPasswordResetToken passwordResetToken=forgotPasswordResetTokenRepository.findByToken(token).orElseThrow(()->new TokenNotFoundException("Token not found"));
        if(passwordResetToken.getExpiresAt().isBefore(LocalDateTime.now()))throw new InvalidTokenException("Reset Link is expired");
        User user=passwordResetToken.getUser();
        user.setPassword(passwordEncoder.encode(newPassword));
        userRepository.save(user);
        refreshTokenRepository.revokeAllByUser(user);
        forgotPasswordResetTokenRepository.deleteByUser(user);

    }





    private String buildResetPasswordUrl(String token) {
        return baseUrl + "/auth/reset-password?token=" + token;
    }


}
