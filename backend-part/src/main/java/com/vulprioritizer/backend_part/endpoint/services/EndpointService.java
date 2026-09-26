package com.vulprioritizer.backend_part.endpoint.services;

import com.vulprioritizer.backend_part.common.exception.OperationNotAllowedException;
import com.vulprioritizer.backend_part.common.exception.ResourceNotFoundException;
import com.vulprioritizer.backend_part.endpoint.dto.response.DiscoveryResponse;
import com.vulprioritizer.backend_part.endpoint.entity.Endpoint;
import com.vulprioritizer.backend_part.endpoint.entity.MethodType;
import com.vulprioritizer.backend_part.endpoint.repository.EndpointRepository;
import com.vulprioritizer.backend_part.targets.entity.Target;
import com.vulprioritizer.backend_part.targets.repository.TargetRepository;
import com.vulprioritizer.backend_part.user.entity.User;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.net.URI;
import java.net.URISyntaxException;
import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
@Slf4j
public class EndpointService {

    private final TargetRepository targetRepository;
    private final EndpointRepository endpointRepository;
    private final CrawlerService crawlerService;

    public DiscoveryResponse discoverEndpoints(Long targetId, User user) {
        Target target = getTargetAndCheckOwnership(user, targetId);

        long start = System.currentTimeMillis();
        CrawlerService.CrawlResult result = crawlerService.crawl(target.getUrl());

        int newEndpoints = 0;
        for (CrawlerService.DiscoveredUrl discovered : result.discoveredUrls()) {
            if (upsertEndpoint(target, discovered.url())) {
                newEndpoints++;
            }
        }

        long durationMs = System.currentTimeMillis() - start;
        log.info("Endpoint discovery for target {}: {} pages visited, {} URLs discovered, {} new endpoints ({} ms)",
                targetId, result.pagesVisited(), result.discoveredUrls().size(), newEndpoints, durationMs);

        return new DiscoveryResponse(
                result.pagesVisited(),
                result.discoveredUrls().size(),
                newEndpoints,
                durationMs,
                result.status().name()
        );
    }

    private Target getTargetAndCheckOwnership(User user, Long targetId) {
        Target target = targetRepository.findByIdAndDeletedFalse(targetId)
                .orElseThrow(() -> new ResourceNotFoundException("Target not found: " + targetId));

        if (!target.getProject().getUser().getId().equals(user.getId())) {
            throw new OperationNotAllowedException("You are not allowed to perform this action");
        }
        return target;
    }

    private boolean upsertEndpoint(Target target, String url) {
        String path = extractPath(url);
        if (path == null) {
            return false;
        }

        Endpoint existing = endpointRepository
                .findByTargetAndPathAndMethod(target, path, MethodType.GET)
                .orElse(null);

        LocalDateTime now = LocalDateTime.now();
        if (existing != null) {
            existing.setActive(true);
            existing.setUrl(url);
            existing.setUpdatedAt(now);
            endpointRepository.save(existing);
            return false;
        }

        Endpoint endpoint = new Endpoint();
        endpoint.setTarget(target);
        endpoint.setPath(path);
        endpoint.setMethod(MethodType.GET);
        endpoint.setUrl(url);
        endpoint.setCreatedAt(now);
        endpoint.setUpdatedAt(now);
        endpoint.setActive(true);
        endpointRepository.save(endpoint);
        return true;
    }

    private String extractPath(String url) {
        try {
            URI uri = new URI(url);
            String path = uri.getPath();
            return path == null || path.isEmpty() ? "/" : path;
        } catch (URISyntaxException e) {
            return null;
        }
    }
}
