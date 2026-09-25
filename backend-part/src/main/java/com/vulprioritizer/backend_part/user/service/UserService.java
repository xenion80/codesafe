package com.vulprioritizer.backend_part.user.service;


import com.vulprioritizer.backend_part.auth.entity.EmailVerificationToken;
import com.vulprioritizer.backend_part.auth.repository.EmailVerificationTokenRepository;
import com.vulprioritizer.backend_part.auth.repository.RefreshTokenRepository;
import com.vulprioritizer.backend_part.auth.services.EmailService;
import com.vulprioritizer.backend_part.common.exception.IdentityAlreadyExistException;
import com.vulprioritizer.backend_part.user.dto.request.ModifyUserDetailRequest;
import com.vulprioritizer.backend_part.user.dto.request.SignUpInputModel;
import com.vulprioritizer.backend_part.user.dto.response.UserResponse;
import com.vulprioritizer.backend_part.user.entity.Role;
import com.vulprioritizer.backend_part.user.entity.User;
import com.vulprioritizer.backend_part.user.repository.UserRepository;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.modelmapper.ModelMapper;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.core.Authentication;

import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.Optional;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class UserService implements UserDetailsService {
    private final UserRepository userRepository;
    private final ModelMapper modelMapper;
    private final EmailService emailService;
    private final PasswordEncoder passwordEncoder;
    private final EmailVerificationTokenRepository tokenRepository;
    private final RefreshTokenRepository refreshTokenRepository;

    @Override
    public UserDetails loadUserByUsername(String email) throws UsernameNotFoundException {
        return userRepository.findByEmail(email).orElseThrow(()->new UsernameNotFoundException("User of this email not found"));
    }
    private String buildVerifyEmailUrl(String token){

        return "http://localhost:8080/auth/verify-email?token=" + token;
    }
    public User getUserById(Long userId){
        return userRepository.findById(userId).orElseThrow(()->new BadCredentialsException("Userid not found"));
    }
    @Transactional
    public UserResponse signUp(SignUpInputModel signUpInputModel){
        Optional<User> user=userRepository.findByEmail(signUpInputModel.getEmail());
        if(user.isPresent()){
            throw new IdentityAlreadyExistException("User with this email already exists: "+signUpInputModel.getEmail());
        }
        User user1=modelMapper.map(signUpInputModel,User.class);
        user1.setPassword(passwordEncoder.encode(signUpInputModel.getPassword()));
        user1.setEnabled(false);
        user1.setRole(Role.USER);
        user1.setEmailVerified(false);

        User saved=userRepository.save(user1);
        String token= UUID.randomUUID().toString();
        EmailVerificationToken emailVerificationToken =new EmailVerificationToken();
        emailVerificationToken.setToken(token);
        emailVerificationToken.setUser(saved);
        emailVerificationToken.setExpiresAt(LocalDateTime.now().plusHours(20));
        tokenRepository.save(emailVerificationToken);
        String verifyEmail=buildVerifyEmailUrl(token);
        emailService.sendMail(
                saved.getEmail(),
                "Verify your email",
                "Click here: "+verifyEmail

        );
        return modelMapper.map(saved,UserResponse.class);

    }

    public UserResponse modify(Authentication authentication, ModifyUserDetailRequest request) {

        User user=(User) authentication.getPrincipal();
        user.setName(request.getName());
        user.setEmail(request.getEmail());
        userRepository.save(user);
        return modelMapper.map(user,UserResponse.class);
    }

    @Transactional
    public void deleteUser(Authentication authentication) {

        User user=(User) authentication.getPrincipal();

        user.setActive(false);
        user.setEnabled(false);
        refreshTokenRepository.revokeAllByUser(user);
        userRepository.save(user);

    }
}
