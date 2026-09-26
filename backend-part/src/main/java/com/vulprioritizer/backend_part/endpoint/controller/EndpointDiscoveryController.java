package com.vulprioritizer.backend_part.endpoint.controller;

import com.vulprioritizer.backend_part.common.response.ApiResponse;
import com.vulprioritizer.backend_part.endpoint.dto.response.DiscoveryResponse;
import com.vulprioritizer.backend_part.endpoint.services.EndpointService;
import com.vulprioritizer.backend_part.user.entity.User;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
public class EndpointDiscoveryController {

    private final EndpointService endpointService;

    @PostMapping("/targets/{targetId}/discover")
    public ResponseEntity<ApiResponse<DiscoveryResponse>> discoverEndpoints(
            Authentication authentication,
            @PathVariable Long targetId) {

        User user = (User) authentication.getPrincipal();
        DiscoveryResponse response = endpointService.discoverEndpoints(targetId, user);
        return ResponseEntity.ok(
                ApiResponse.success("Endpoint discovery completed", response)
        );
    }
}
