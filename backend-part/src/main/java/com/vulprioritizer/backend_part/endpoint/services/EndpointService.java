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

/**
 * Application service for endpoint discovery: authorization (User -> Project -> Target),
 * crawl orchestration and persistence of discovered URLs as GET {@link Endpoint} rows.
 *
 * <p>Crawling itself (fetch, parse, BFS) lives in {@link CrawlerService}; this class only
 * decides <em>what</em> gets persisted and reports the summary.</p>
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class EndpointService {

    private final TargetRepository targetRepository;
    private final EndpointRepository endpointRepository;
    private final CrawlerService crawlerService;

    /**
     * Crawls the target's base URL and upserts every discovered page as a GET endpoint.
     *
     * @throws ResourceNotFoundException  when the target does not exist (or is deleted)
     * @throws OperationNotAllowedException when the authenticated user does not own the
     *                                    target's project
     */
    // Deliberately NOT @Transactional: the crawl performs network I/O that can take
    // minutes, and holding a DB connection open for it would starve the pool.
    // Each endpoint upsert below commits in its own implicit transaction.
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

    /** Loads the target and enforces User -> Project -> Target ownership. */
    private Target getTargetAndCheckOwnership(User user, Long targetId) {
        Target target = targetRepository.findByIdAndDeletedFalse(targetId)
                .orElseThrow(() -> new ResourceNotFoundException("Target not found: " + targetId));

        if (!target.getProject().getUser().getId().equals(user.getId())) {
            throw new OperationNotAllowedException("You are not allowed to perform this action");
        }
        return target;
    }

    /**
     * Creates the GET endpoint for {@code url} if no endpoint with the same
     * (target, path, method) exists yet; otherwise re-activates the existing row and
     * refreshes its updatedAt.
     *
     * @return true when a new endpoint row was inserted
     */
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

    /** Path (without query/fragment) of a normalized URL; null when malformed. */
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
