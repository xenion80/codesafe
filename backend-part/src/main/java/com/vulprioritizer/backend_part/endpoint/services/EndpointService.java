package com.vulprioritizer.backend_part.endpoint.services;

import com.vulprioritizer.backend_part.common.exception.ResourceNotFoundException;
import com.vulprioritizer.backend_part.endpoint.dto.response.EndpointResponse;
import com.vulprioritizer.backend_part.targets.entity.Target;
import com.vulprioritizer.backend_part.targets.repository.TargetRepository;
import com.vulprioritizer.backend_part.user.entity.User;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class EndpointService {
    private final TargetRepository targetRepository;

    public List<EndpointResponse> discoverUrl(Long targetId, User user) {
        Target target=targetRepository.findById(targetId).orElseThrow(()->new ResourceNotFoundException("Target with this id not found"));

        if(!target.getProject().getUser().getId().equals(user.getId())){
            throw new BadCredentialsException("bad credentials");
        }
    }
}
