package com.vulprioritizer.backend_part.user.service;


import com.vulprioritizer.backend_part.auth.repository.RefreshTokenRepository;
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

import java.util.Optional;

@Service
@RequiredArgsConstructor
public class UserService implements UserDetailsService {
    private final UserRepository userRepository;
    private final ModelMapper modelMapper;
    private final PasswordEncoder passwordEncoder;
    private final RefreshTokenRepository refreshTokenRepository;

    @Override
    public UserDetails loadUserByUsername(String email) throws UsernameNotFoundException {
        return userRepository.findByEmail(email).orElseThrow(()->new UsernameNotFoundException("User of this email not found"));
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
        // Email verification removed along with the SMTP dependency:
        // accounts are active immediately after sign-up.
        user1.setEnabled(true);
        user1.setRole(Role.USER);

        User saved=userRepository.save(user1);
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
