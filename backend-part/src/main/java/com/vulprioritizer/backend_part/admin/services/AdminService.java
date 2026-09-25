package com.vulprioritizer.backend_part.admin.services;


import com.vulprioritizer.backend_part.auth.repository.RefreshTokenRepository;
import com.vulprioritizer.backend_part.common.exception.ResourceNotFoundException;
import com.vulprioritizer.backend_part.user.dto.response.UserResponse;
import com.vulprioritizer.backend_part.user.entity.User;
import com.vulprioritizer.backend_part.user.repository.UserRepository;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.modelmapper.ModelMapper;
import org.springframework.data.domain.Page;
import org.springframework.stereotype.Service;

import org.springframework.data.domain.Pageable;

@Service
@RequiredArgsConstructor
public class AdminService {
    private final UserRepository userRepository;
    private final ModelMapper modelMapper;
    private final RefreshTokenRepository refreshTokenRepository;

    @Transactional
    public UserResponse disableUser(Long id) {
        User user=userRepository.findById(id).orElseThrow(()->new ResourceNotFoundException("User with this id not found"));
        user.setEnabled(false);
        user.setActive(false);
        userRepository.save(user);
        refreshTokenRepository.revokeAllByUser(user);
        return modelMapper.map(user,UserResponse.class);
    }

    public UserResponse enableUser(Long id) {
        User user=userRepository.findById(id).orElseThrow(()->new ResourceNotFoundException("User with this id not found"));
        user.setEnabled(true);
        user.setActive(true);
        userRepository.save(user);
        return modelMapper.map(user,UserResponse.class);
    }

    public UserResponse listOneUser(Long id) {
        User user=userRepository.findById(id).orElseThrow(()->new ResourceNotFoundException("User with this id not found"));

        return modelMapper.map(user,UserResponse.class);
    }

    public Page<UserResponse> listAllUsers(Pageable pageable) {
        return userRepository.findAll(pageable)
                .map(users->modelMapper.map(users,UserResponse.class));
    }
}
