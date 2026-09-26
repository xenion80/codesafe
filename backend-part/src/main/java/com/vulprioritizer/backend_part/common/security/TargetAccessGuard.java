package com.vulprioritizer.backend_part.common.security;

import com.vulprioritizer.backend_part.common.exception.OperationNotAllowedException;
import com.vulprioritizer.backend_part.common.exception.ResourceNotFoundException;
import com.vulprioritizer.backend_part.targets.entity.Target;
import com.vulprioritizer.backend_part.targets.repository.TargetRepository;
import com.vulprioritizer.backend_part.user.entity.User;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class TargetAccessGuard {

    private final TargetRepository targetRepository;

    public Target getOwnedTargetOrThrow(User user, Long targetId) {
        Target target = targetRepository.findByIdAndDeletedFalse(targetId)
                .orElseThrow(() -> new ResourceNotFoundException("Target not found: " + targetId));

        if (!target.getProject().getUser().getId().equals(user.getId())) {
            throw new OperationNotAllowedException("You are not allowed to perform this action");
        }
        return target;
    }

    public boolean isOwnedBy(User user, Target target) {
        return target.getProject().getUser().getId().equals(user.getId());
    }
}
