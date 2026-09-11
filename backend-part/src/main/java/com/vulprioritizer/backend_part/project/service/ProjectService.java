package com.vulprioritizer.backend_part.project.service;

import com.vulprioritizer.backend_part.common.exception.OperationNotAllowedException;
import com.vulprioritizer.backend_part.common.exception.ResourceNotFoundException;
import com.vulprioritizer.backend_part.project.dto.request.CreateProjectRequest;
import com.vulprioritizer.backend_part.project.dto.response.ProjectResponse;
import com.vulprioritizer.backend_part.project.entity.Project;
import com.vulprioritizer.backend_part.project.repository.ProjectRepository;
import com.vulprioritizer.backend_part.user.entity.User;
import com.vulprioritizer.backend_part.user.repository.UserRepository;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.modelmapper.ModelMapper;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
public class ProjectService {
    private final ProjectRepository projectRepository;
    private final UserRepository userRepository;
    private final ModelMapper modelMapper;

    public ProjectResponse createProject(@Valid CreateProjectRequest request, User user) {
        Project project=Project.builder()
                .name(request.getName())
                .description(request.getDescription())
                .user(user)
                .createdAt(LocalDateTime.now())
                .build();
        Project saved=projectRepository.save(project);
        return modelMapper.map(saved,ProjectResponse.class);
    }

    public Page<ProjectResponse> getAllProject(Pageable pageable, User user) {
        return projectRepository.findByUserAndDeletedFalse(user,pageable);
    }

    public ProjectResponse getProject(long projectId, User user) {
        Project project=projectRepository.findByIdAndDeletedFalse(projectId).orElseThrow(()->new ResourceNotFoundException("resource with this project id is not found"));
        if (!project.getUser().getId().equals(user.getId())){
            throw new OperationNotAllowedException("Bad request");
        }
        return modelMapper.map(project,ProjectResponse.class);
    }

    public void  deleteProject(long projectId, User user) {
        Project project=projectRepository.findByIdAndDeletedFalse(projectId).orElseThrow(()->new ResourceNotFoundException("resource with this project id is not found"));
        if (!project.getUser().getId().equals(user.getId())){
            throw new OperationNotAllowedException("Bad request");
        }
        project.setDeleted(true);
        projectRepository.save(project);
    }

    public ProjectResponse editProject(long projectId, CreateProjectRequest request, User user)  {
        Project project=projectRepository.findByIdAndDeletedFalse(projectId).orElseThrow(()->new ResourceNotFoundException("resource with this project id is not found"));

        if (!project.getUser().getId().equals(user.getId())){
            throw new OperationNotAllowedException("Bad request");
        }
        project.setName(request.getName());
        project.setDescription(request.getDescription());
        Project saved=projectRepository.save(project);
        return modelMapper.map(saved,ProjectResponse.class);
    }
}
