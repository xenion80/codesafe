package com.vulprioritizer.backend_part.endpoint.services;

import com.vulprioritizer.backend_part.common.exception.OperationNotAllowedException;
import com.vulprioritizer.backend_part.common.exception.ResourceNotFoundException;
import com.vulprioritizer.backend_part.endpoint.dto.response.DiscoveryResponse;
import com.vulprioritizer.backend_part.endpoint.entity.Endpoint;
import com.vulprioritizer.backend_part.endpoint.entity.MethodType;
import com.vulprioritizer.backend_part.endpoint.repository.EndpointRepository;
import com.vulprioritizer.backend_part.endpoint.services.PageFetcher.PageResult;
import com.vulprioritizer.backend_part.project.entity.Project;
import com.vulprioritizer.backend_part.targets.entity.Target;
import com.vulprioritizer.backend_part.targets.repository.TargetRepository;
import com.vulprioritizer.backend_part.user.entity.Role;
import com.vulprioritizer.backend_part.user.entity.User;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class EndpointServiceTest {

    @Mock
    private TargetRepository targetRepository;

    @Mock
    private EndpointRepository endpointRepository;

    @Mock
    private PageFetcher pageFetcher;

    private EndpointService endpointService;

    private User owner;
    private User otherUser;
    private Target target;

    @BeforeEach
    void setUp() {
        CrawlerService crawlerService = new CrawlerService(pageFetcher);
        endpointService = new EndpointService(targetRepository, endpointRepository, crawlerService);

        // Default: any URL the crawler fetches answers with an empty page unless a
        // test stubs something more specific.
        lenient().when(pageFetcher.fetch(any()))
                .thenReturn(PageResult.success(200, ""));
        // Default: no endpoint exists yet for any (target, path, method).
        lenient().when(endpointRepository.findByTargetAndPathAndMethod(any(), any(), any()))
                .thenReturn(Optional.empty());

        owner = new User();
        owner.setId(1L);
        owner.setRole(Role.USER);
        otherUser = new User();
        otherUser.setId(2L);
        otherUser.setRole(Role.USER);

        Project project = new Project();
        project.setId(10L);
        project.setUser(owner);

        target = new Target();
        target.setId(100L);
        target.setUrl("https://test.local");
        target.setProject(project);
    }

    private static PageResult page(String html) {
        return PageResult.success(200, html);
    }

    @Test
    void rejectsUserWhoDoesNotOwnTheTarget() {
        when(targetRepository.findByIdAndDeletedFalse(100L)).thenReturn(Optional.of(target));

        assertThatThrownBy(() -> endpointService.discoverEndpoints(100L, otherUser))
                .isInstanceOf(OperationNotAllowedException.class);
        verify(pageFetcher, never()).fetch(any());
    }

    @Test
    void throwsWhenTargetDoesNotExist() {
        when(targetRepository.findByIdAndDeletedFalse(404L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> endpointService.discoverEndpoints(404L, owner))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    void persistsDiscoveredUrlsAsGetEndpointsWithoutDuplicates() {
        when(targetRepository.findByIdAndDeletedFalse(100L)).thenReturn(Optional.of(target));
        when(pageFetcher.fetch("https://test.local/"))
                .thenReturn(page("<a href=\"/login\"></a><a href=\"/products\"></a>"));

        DiscoveryResponse response = endpointService.discoverEndpoints(100L, owner);

        assertThat(response.getPagesVisited()).isEqualTo(3);
        assertThat(response.getEndpointsDiscovered()).isEqualTo(3);
        assertThat(response.getNewEndpoints()).isEqualTo(3);
        assertThat(response.getStatus()).isEqualTo(CrawlerService.CrawlStatus.COMPLETED.name());
        assertThat(response.getDurationMs()).isGreaterThanOrEqualTo(0);

        ArgumentCaptor<Endpoint> captor = ArgumentCaptor.forClass(Endpoint.class);
        verify(endpointRepository, times(3)).save(captor.capture());
        List<Endpoint> saved = captor.getAllValues();
        assertThat(saved).allSatisfy(endpoint -> {
            assertThat(endpoint.getTarget()).isEqualTo(target);
            assertThat(endpoint.getMethod()).isEqualTo(MethodType.GET);
            assertThat(endpoint.isActive()).isTrue();
            assertThat(endpoint.getUrl()).startsWith("https://test.local");
            assertThat(endpoint.getCreatedAt()).isNotNull();
            assertThat(endpoint.getUpdatedAt()).isNotNull();
        });
        assertThat(saved)
                .extracting(Endpoint::getPath)
                .containsExactlyInAnyOrder("/", "/login/", "/products/");
    }

    @Test
    void doesNotInsertDuplicateEndpointForExistingTargetPathMethod() {
        when(targetRepository.findByIdAndDeletedFalse(100L)).thenReturn(Optional.of(target));
        when(pageFetcher.fetch("https://test.local/")).thenReturn(page("<a href=\"/login\"></a>"));

        Endpoint existingLogin = new Endpoint();
        existingLogin.setTarget(target);
        existingLogin.setPath("/login/");
        existingLogin.setMethod(MethodType.GET);
        existingLogin.setActive(false);
        existingLogin.setCreatedAt(LocalDateTime.now().minusDays(1));
        existingLogin.setUpdatedAt(LocalDateTime.now().minusDays(1));

        Endpoint existingRoot = new Endpoint();
        existingRoot.setTarget(target);
        existingRoot.setPath("/");
        existingRoot.setMethod(MethodType.GET);
        existingRoot.setActive(true);

        when(endpointRepository.findByTargetAndPathAndMethod(target, "/login/", MethodType.GET))
                .thenReturn(Optional.of(existingLogin));
        when(endpointRepository.findByTargetAndPathAndMethod(target, "/", MethodType.GET))
                .thenReturn(Optional.of(existingRoot));

        DiscoveryResponse response = endpointService.discoverEndpoints(100L, owner);

        // Both discovered pages ("/" and "/login/") already exist -> re-activated only.
        assertThat(response.getEndpointsDiscovered()).isEqualTo(2);
        assertThat(response.getNewEndpoints()).isZero();
        ArgumentCaptor<Endpoint> captor = ArgumentCaptor.forClass(Endpoint.class);
        verify(endpointRepository, times(2)).save(captor.capture());
        assertThat(captor.getAllValues())
                .extracting(Endpoint::getPath, Endpoint::isActive)
                .containsExactlyInAnyOrder(
                        org.assertj.core.groups.Tuple.tuple("/", true),
                        org.assertj.core.groups.Tuple.tuple("/login/", true));
        assertThat(existingLogin.getUrl()).isEqualTo("https://test.local/login/");
    }

    @Test
    void failedBasePageDoesNotInsertAnything() {
        when(targetRepository.findByIdAndDeletedFalse(100L)).thenReturn(Optional.of(target));
        when(pageFetcher.fetch("https://test.local/"))
                .thenReturn(PageResult.failure(PageFetcher.FetchStatus.TIMEOUT, -1));

        DiscoveryResponse response = endpointService.discoverEndpoints(100L, owner);

        assertThat(response.getStatus()).isEqualTo(CrawlerService.CrawlStatus.START_URL_UNREACHABLE.name());
        assertThat(response.getEndpointsDiscovered()).isZero();
        assertThat(response.getNewEndpoints()).isZero();
        verify(endpointRepository, never()).save(any());
    }
}
