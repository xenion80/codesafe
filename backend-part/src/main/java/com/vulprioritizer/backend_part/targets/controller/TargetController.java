package com.vulprioritizer.backend_part.targets.controller;

import com.vulprioritizer.backend_part.common.exception.OperationNotAllowedException;
import com.vulprioritizer.backend_part.common.response.ApiResponse;
import com.vulprioritizer.backend_part.targets.dto.request.CreateTargetRequest;
import com.vulprioritizer.backend_part.targets.dto.response.CreateTargetResponse;
import com.vulprioritizer.backend_part.targets.services.TargetService;
import com.vulprioritizer.backend_part.user.entity.User;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/targets")
@RequiredArgsConstructor
public class TargetController {
    private final TargetService targetService;


    @GetMapping("/{targetId}")
    public ResponseEntity<ApiResponse<CreateTargetResponse>> getTarget(
            Authentication authentication,
            @PathVariable Long targetId) {

        User user=(User) authentication.getPrincipal();
        CreateTargetResponse targetResponse=targetService.getTarget(user,targetId);
        return ResponseEntity.ok(
                ApiResponse.success("Target found:",targetResponse)
        );


    }
    @PutMapping("/{targetId}")
    public ResponseEntity<ApiResponse<CreateTargetResponse>> editTarget(
            Authentication authentication,
            @PathVariable Long targetId,
            @Valid @RequestBody CreateTargetRequest request
            ){
        User user=(User) authentication.getPrincipal();
        CreateTargetResponse targetResponse=targetService.editTarget(user,targetId,request);
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
}
