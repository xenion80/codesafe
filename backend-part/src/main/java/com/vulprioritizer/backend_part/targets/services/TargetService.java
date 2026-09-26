package com.vulprioritizer.backend_part.targets.services;

import com.vulprioritizer.backend_part.common.exception.OperationNotAllowedException;
import com.vulprioritizer.backend_part.common.exception.ResourceNotFoundException;
import com.vulprioritizer.backend_part.project.entity.Project;
import com.vulprioritizer.backend_part.project.repository.ProjectRepository;
import com.vulprioritizer.backend_part.targets.dto.request.CreateTargetRequest;
import com.vulprioritizer.backend_part.targets.dto.response.TargetResponse;
import com.vulprioritizer.backend_part.targets.entity.Target;
import com.vulprioritizer.backend_part.targets.repository.TargetRepository;
import com.vulprioritizer.backend_part.user.entity.User;
import lombok.RequiredArgsConstructor;
import org.modelmapper.ModelMapper;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
public class TargetService {
    private final ProjectRepository projectRepository;
    private final TargetRepository targetRepository;
    private final ModelMapper modelMapper;

    public TargetResponse createTarget(CreateTargetRequest request, Long projectId, User user) {
        Project project=getProjectAndCheckDetail(user,projectId);

            Target target=Target.builder()
                    .name(request.getName())
                    .description(request.getDescription())
                    .url(request.getBaseUrl())
                    .type(request.getType())
                    .project(project)
                    .created_at(LocalDateTime.now())
                    .updated_at(LocalDateTime.now())
                    .deleted(false)
                    .build();
            Target saved=targetRepository.save(target);
            return modelMapper.map(saved, TargetResponse.class);

    }

    public Page<TargetResponse> getTargetsAssociatedWithProject(Pageable pageable, Long projectId, User user) {
        Project project=getProjectAndCheckDetail(user,projectId);

        return targetRepository.findByProjectAndDeletedFalse(pageable,project);
    }

    public TargetResponse getTarget(User user, Long targetId) {
        Target target=getTargetAndCheckDetail(user, targetId);

        return modelMapper.map(target, TargetResponse.class);

    }

    public TargetResponse editTarget(User user, Long targetId, CreateTargetRequest request) {

        Target target=getTargetAndCheckDetail(user,targetId);
        target.setName(request.getName());
        target.setDescription(request.getDescription());
        target.setType(request.getType());
        target.setUrl(request.getBaseUrl());
        target.setUpdated_at(LocalDateTime.now());
        Target saved=targetRepository.save(target);

        return modelMapper.map(saved, TargetResponse.class);

    }

    public void deleteTarget(User user, Long targetId) {
        Target target=getTargetAndCheckDetail(user,targetId);
        target.setDeleted(true);
        targetRepository.save(target);

    }
    private Target getTargetAndCheckDetail(User user,Long targetId){
        Target target=targetRepository.findByIdAndDeletedFalse(targetId).orElseThrow(()->new ResourceNotFoundException("Target not found: " + targetId));

        if(!target.getProject().getUser().getId().equals(user.getId())){
            throw new OperationNotAllowedException("You are not allowed to perform this action");
        }
        return target;

    }

    private Project getProjectAndCheckDetail(User user,Long projectId){
        Project project=projectRepository.findByIdAndDeletedFalse(projectId).orElseThrow(()->new ResourceNotFoundException("Project not found"));

        if(!project.getUser().getId().equals(user.getId())){
            throw new OperationNotAllowedException("You are not allowed to perform this action");
        }
        return project;
    }

}
