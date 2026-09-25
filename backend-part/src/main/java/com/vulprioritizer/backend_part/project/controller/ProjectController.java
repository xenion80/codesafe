package com.vulprioritizer.backend_part.project.controller;

import com.vulprioritizer.backend_part.common.exception.OperationNotAllowedException;
import com.vulprioritizer.backend_part.common.response.ApiResponse;
import com.vulprioritizer.backend_part.project.dto.request.CreateProjectRequest;
import com.vulprioritizer.backend_part.project.dto.response.ProjectResponse;
import com.vulprioritizer.backend_part.project.service.ProjectService;
import com.vulprioritizer.backend_part.targets.dto.request.CreateTargetRequest;
import com.vulprioritizer.backend_part.targets.dto.response.TargetResponse;
import com.vulprioritizer.backend_part.targets.services.TargetService;
import com.vulprioritizer.backend_part.user.entity.User;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/projects")
@RequiredArgsConstructor
public class ProjectController {
    private final ProjectService projectService;
    private final TargetService targetService;

    @PostMapping("/create")
    public ResponseEntity<ApiResponse<ProjectResponse>> createProject(
            @Valid@RequestBody CreateProjectRequest request,
            Authentication authentication
            ){
        User user=(User) authentication.getPrincipal();
        if (user == null) {
            throw new OperationNotAllowedException("User is not authenticated");
        }
        ProjectResponse project=projectService.createProject(request,user);
        return ResponseEntity.ok(
                ApiResponse.success("Project created successfully",project)
        );
    }


    @GetMapping
    public ResponseEntity<ApiResponse<Page<ProjectResponse>>> getAllProject(
            @PageableDefault(size = 10,sort = "id") Pageable pageable,
            Authentication authentication
            ){
        User user=(User) authentication.getPrincipal();
        if (user == null) {
            throw new OperationNotAllowedException("User is not authenticated");
        }
        Page<ProjectResponse> project=projectService.getAllProject(pageable,user);
        return ResponseEntity.ok(
                ApiResponse.success("Retrieved all project Detail",project)
        );
    }
    @GetMapping("/{projectId}")
    public ResponseEntity<ApiResponse<ProjectResponse>> getProject(@PathVariable long projectId, Authentication authentication){
        User user=(User)authentication.getPrincipal();
        if (user == null) {
            throw new OperationNotAllowedException("User is not authenticated");
        }
        ProjectResponse project=projectService.getProject(projectId,user);
        return ResponseEntity.ok(
                ApiResponse.success("Successfully retrieved project detail",project)
        );
    }

    @DeleteMapping("/{projectId}")
    public ResponseEntity<ApiResponse<Void>> deleteProject(@PathVariable long projectId,Authentication authentication){
        User user=(User) authentication.getPrincipal();
        if (user == null) {
            throw new OperationNotAllowedException("User is not authenticated");
        }
        projectService.deleteProject(projectId,user);
        return ResponseEntity.ok(
                ApiResponse.success("Successfully deleted project detail",null)
        );
    }

    @PutMapping("/{projectId}")
    public ResponseEntity<ApiResponse<ProjectResponse>> editProject(@PathVariable long projectId,@Valid@RequestBody CreateProjectRequest request, Authentication authentication){
        User user=(User) authentication.getPrincipal();
        if (user == null) {
            throw new OperationNotAllowedException("User is not authenticated");
        }
        ProjectResponse project= projectService.editProject(projectId,request,user);

        return ResponseEntity.ok(
                ApiResponse.success("Successfully updated project",project)
        );
    }


    @PostMapping("{/projectId]/targets}")
    public ResponseEntity<ApiResponse<TargetResponse>> targetDetail(@Valid @RequestBody CreateTargetRequest request, @PathVariable Long projectId, Authentication authentication){
        User user=(User) authentication.getPrincipal();
        TargetResponse targetResponse=targetService.createTarget(request,projectId,user);
        return ResponseEntity.ok(
                ApiResponse.success("Target created successfully",targetResponse)
        );
    }


    @GetMapping("{/projectId]/targets}")
    public ResponseEntity<ApiResponse<Page<TargetResponse>>> getTargetsAssociatedWithProject (
            @PathVariable Long projectId,
            Authentication authentication,
            @PageableDefault Pageable pageable){
        User user=(User) authentication.getPrincipal();
        Page<TargetResponse> targetResponse=targetService.getTargetsAssociatedWithProject(pageable,projectId,user);
        return ResponseEntity.ok(
                ApiResponse.success("Successfully retrieved all target information",targetResponse)
        );
    }


}
