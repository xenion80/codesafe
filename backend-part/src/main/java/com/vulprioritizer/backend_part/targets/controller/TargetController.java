package com.vulprioritizer.backend_part.targets.controller;

import com.vulprioritizer.backend_part.common.response.ApiResponse;
import com.vulprioritizer.backend_part.endpoint.dto.request.EndpointRequest;
import com.vulprioritizer.backend_part.endpoint.dto.response.EndpointResponse;
import com.vulprioritizer.backend_part.endpoint.services.EndpointService;
import com.vulprioritizer.backend_part.targets.dto.request.CreateTargetRequest;
import com.vulprioritizer.backend_part.targets.dto.response.TargetResponse;
import com.vulprioritizer.backend_part.targets.services.TargetService;
import com.vulprioritizer.backend_part.user.entity.User;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/targets")
@RequiredArgsConstructor
public class TargetController {
    private final TargetService targetService;
    private final EndpointService endpointService;


    @GetMapping("/{targetId}")
    public ResponseEntity<ApiResponse<TargetResponse>> getTarget(
            Authentication authentication,
            @PathVariable Long targetId) {

        User user=(User) authentication.getPrincipal();
        TargetResponse targetResponse=targetService.getTarget(user,targetId);
        return ResponseEntity.ok(
                ApiResponse.success("Target found:",targetResponse)
        );


    }
    @PutMapping("/{targetId}")
    public ResponseEntity<ApiResponse<TargetResponse>> editTarget(
            Authentication authentication,
            @PathVariable Long targetId,
            @Valid @RequestBody CreateTargetRequest request
            ){
        User user=(User) authentication.getPrincipal();
        TargetResponse targetResponse=targetService.editTarget(user,targetId,request);
        return ResponseEntity.ok(
                ApiResponse.success("Target edited successfully",targetResponse)
        );
    }
    @DeleteMapping("/{targetId}")
    public ResponseEntity<ApiResponse<Void>> deleteTarget(
            Authentication authentication,
            @PathVariable Long targetId
    ){
        User user=(User) authentication.getPrincipal();
        targetService.deleteTarget(user,targetId);
        return ResponseEntity.ok(
                ApiResponse.success("Target deleted successfully",null)
        );
    }

//    @PostMapping("/{targetId}/discover")
//    public ResponseEntity<ApiResponse<List<EndpointResponse>>> discoverUrl(@PathVariable Long targetId, Authentication authentication){
//        User user=(User) authentication.getPrincipal();
//        List<EndpointResponse> endpoints=endpointService.discoverUrl(targetId,user);

//        return ResponseEntity.ok(
//                ApiResponse.success("Retrived endpoints successfully",endpoints)
//        );
//
//
//    }


}
