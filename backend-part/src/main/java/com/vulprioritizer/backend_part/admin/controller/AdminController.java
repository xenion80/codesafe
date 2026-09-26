package com.vulprioritizer.backend_part.admin.controller;

import com.vulprioritizer.backend_part.admin.services.AdminService;
import com.vulprioritizer.backend_part.common.response.ApiResponse;
import com.vulprioritizer.backend_part.user.dto.response.UserResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import org.springframework.data.domain.Pageable;

@RestController
@RequestMapping("/admin")
@RequiredArgsConstructor
public class AdminController {
    private final AdminService adminService;

    @GetMapping("/users")
    public ResponseEntity<ApiResponse<Page<UserResponse>>> listAllUsers(
            @PageableDefault(size = 10,sort = "id")Pageable pageable
            ){
        Page<UserResponse> users=adminService.listAllUsers(pageable);
        return ResponseEntity.ok(
                ApiResponse.success("List of All Users : ",users));
    }

    @GetMapping("/users/{id}")
    public ResponseEntity<ApiResponse<UserResponse>> listOneUser(@PathVariable Long id){
        UserResponse response=adminService.listOneUser(id);
        return ResponseEntity.ok(
                ApiResponse.success("Retrival successful : ",response)
        );
    }
    @PatchMapping("/users/{id}/disable")
    public ResponseEntity<ApiResponse<UserResponse>> disableUser(@PathVariable Long id){
        UserResponse response=adminService.disableUser(id);
        return ResponseEntity.ok(
                ApiResponse.success("User has been disabled",response)
        );
    }
    @PatchMapping("/users/{id}/enable")
    public ResponseEntity<ApiResponse<UserResponse>> enableUser(@PathVariable Long id){

        UserResponse response=adminService.enableUser(id);
        return ResponseEntity.ok(
                ApiResponse.success("User has been enabled",response)
        );
    }
}
