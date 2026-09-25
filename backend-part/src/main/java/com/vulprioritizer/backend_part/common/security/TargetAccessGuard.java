package com.vulprioritizer.backend_part.common.security;

import com.vulprioritizer.backend_part.common.exception.OperationNotAllowedException;
import com.vulprioritizer.backend_part.common.exception.ResourceNotFoundException;
import com.vulprioritizer.backend_part.targets.entity.Target;
import com.vulprioritizer.backend_part.targets.repository.TargetRepository;
import com.vulprioritizer.backend_part.user.entity.User;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

/**
 * Centralized target-ownership enforcement for every CyberTotal module.
 *
 * <p>Implements the platform-wide ownership chain User -> Project -> Target. New
 * services must use this guard instead of re-implementing the check, so ownership
 * rules stay consistent across Sentinel, Agent Trap and Intelligence APIs.</p>
 */
@Component
@RequiredArgsConstructor
public class TargetAccessGuard {

    private final TargetRepository targetRepository;

    /**
     * Loads the target and verifies the authenticated user owns it through
     * its project.
     *
     * @throws ResourceNotFoundException    when the target does not exist (or is deleted)
     * @throws OperationNotAllowedException when the user does not own the target's project
     */
    public Target getOwnedTargetOrThrow(User user, Long targetId) {
        Target target = targetRepository.findByIdAndDeletedFalse(targetId)
                .orElseThrow(() -> new ResourceNotFoundException("Target not found: " + targetId));

        if (!target.getProject().getUser().getId().equals(user.getId())) {
            throw new OperationNotAllowedException("You are not allowed to perform this action");
        }
        return target;
    }

    /** True when the user owns the given target through its project. */
    public boolean isOwnedBy(User user, Target target) {
        return target.getProject().getUser().getId().equals(user.getId());
    }
}
