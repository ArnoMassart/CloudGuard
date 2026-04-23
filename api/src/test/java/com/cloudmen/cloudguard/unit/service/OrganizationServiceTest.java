package com.cloudmen.cloudguard.unit.service;

import com.cloudmen.cloudguard.domain.model.Organization;
import com.cloudmen.cloudguard.domain.model.User;
import com.cloudmen.cloudguard.repository.OrganizationRepository;
import com.cloudmen.cloudguard.repository.UserRepository;
import com.cloudmen.cloudguard.repository.UserSecurityPreferenceRepository;
import com.cloudmen.cloudguard.service.OrganizationService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;

import java.util.Collections;
import java.util.List;
import java.util.Optional;

import static com.cloudmen.cloudguard.unit.helper.UserTestHelper.createDbUser;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class OrganizationServiceTest {

    @Mock
    private OrganizationRepository organizationRepository;

    @Mock
    private UserRepository userRepository;

    @Mock
    private UserSecurityPreferenceRepository userSecurityPreferenceRepository;

    private OrganizationService organizationService;

    @BeforeEach
    void setUp() {
        organizationService =
                new OrganizationService(organizationRepository, userRepository, userSecurityPreferenceRepository);
        lenient()
                .when(organizationRepository.findIdsOfUnusedFallbackOrganizationsWithoutCustomerId())
                .thenReturn(Collections.emptyList());
    }

    @Test
    void ensureUserLinkedToOrganization_whenMovingFromFallbackToReal_deletesUnusedFallbackOrg() {
        User user = createDbUser("u@example.com", "U", "Ser", "en");
        user.setId(1L);
        user.setOrganizationId(10L);

        Organization fallback = new Organization();
        fallback.setId(10L);
        fallback.setCustomerId(null);
        fallback.setName("Organization (workspace customer id unavailable)");

        Organization real = new Organization();
        real.setId(20L);
        real.setCustomerId("C-abc");
        real.setName("Workspace C-abc");

        when(organizationRepository.findByCustomerId("C-abc")).thenReturn(Optional.of(real));
        when(organizationRepository.findById(10L)).thenReturn(Optional.of(fallback));
        when(userRepository.existsByOrganizationId(10L)).thenReturn(false);

        organizationService.ensureUserLinkedToOrganization(user, "C-abc", null);

        verify(userRepository).save(user);
        verify(userRepository).flush();
        verify(userSecurityPreferenceRepository).deleteByOrganizationId(10L);
        verify(organizationRepository).deleteById(10L);
    }

    @Test
    void ensureUserLinkedToOrganization_whenAnotherUserStillOnFallback_doesNotDeleteOrg() {
        User user = createDbUser("u@example.com", "U", "Ser", "en");
        user.setId(1L);
        user.setOrganizationId(10L);

        Organization fallback = new Organization();
        fallback.setId(10L);
        fallback.setCustomerId(null);

        Organization real = new Organization();
        real.setId(20L);
        real.setCustomerId("C-abc");

        when(organizationRepository.findByCustomerId("C-abc")).thenReturn(Optional.of(real));
        when(organizationRepository.findById(10L)).thenReturn(Optional.of(fallback));
        when(userRepository.existsByOrganizationId(10L)).thenReturn(true);

        organizationService.ensureUserLinkedToOrganization(user, "C-abc", null);

        verify(userRepository).save(user);
        verify(userRepository).flush();
        verifyNoInteractions(userSecurityPreferenceRepository);
        verify(organizationRepository, never()).deleteById(any());
    }

    @Test
    void ensureUserLinkedToOrganization_whenPreviousOrgHasCustomerId_doesNotDelete() {
        User user = createDbUser("u@example.com", "U", "Ser", "en");
        user.setId(1L);
        user.setOrganizationId(10L);

        Organization previous = new Organization();
        previous.setId(10L);
        previous.setCustomerId("C-old");

        Organization real = new Organization();
        real.setId(20L);
        real.setCustomerId("C-abc");

        when(organizationRepository.findByCustomerId("C-abc")).thenReturn(Optional.of(real));
        when(organizationRepository.findById(10L)).thenReturn(Optional.of(previous));

        organizationService.ensureUserLinkedToOrganization(user, "C-abc", null);

        verify(userRepository).save(user);
        verifyNoInteractions(userSecurityPreferenceRepository);
        verify(organizationRepository, never()).deleteById(any());
    }

    @Test
    void ensureUserLinkedToOrganization_whenAlreadyOnTargetOrg_doesNotRunCleanup() {
        User user = createDbUser("u@example.com", "U", "Ser", "en");
        user.setId(1L);
        user.setOrganizationId(20L);

        Organization real = new Organization();
        real.setId(20L);
        real.setCustomerId("C-abc");

        when(organizationRepository.findByCustomerId("C-abc")).thenReturn(Optional.of(real));

        organizationService.ensureUserLinkedToOrganization(user, "C-abc", null);

        verify(userRepository, never()).save(any());
        verify(userRepository, never()).flush();
        verify(organizationRepository, never()).findById(any());
    }

    @Test
    void ensureUserLinkedToOrganization_withCustomerId_sweepsOtherUnusedFallbackOrgsEvenWithoutRelink() {
        User user = createDbUser("u@example.com", "U", "Ser", "en");
        user.setId(1L);
        user.setOrganizationId(20L);

        Organization real = new Organization();
        real.setId(20L);
        real.setCustomerId("C-abc");

        Organization orphan = new Organization();
        orphan.setId(99L);
        orphan.setCustomerId(null);

        when(organizationRepository.findByCustomerId("C-abc")).thenReturn(Optional.of(real));
        when(organizationRepository.findIdsOfUnusedFallbackOrganizationsWithoutCustomerId())
                .thenReturn(List.of(99L));
        when(organizationRepository.findById(99L)).thenReturn(Optional.of(orphan));
        when(userRepository.existsByOrganizationId(99L)).thenReturn(false);

        organizationService.ensureUserLinkedToOrganization(user, "C-abc", null);

        verify(userRepository, never()).save(any());
        verify(userSecurityPreferenceRepository).deleteByOrganizationId(99L);
        verify(organizationRepository).deleteById(99L);
    }

    @Test
    void ensureUserLinkedToOrganization_updatesNameWhenGoogleDisplayNameProvided() {
        User user = createDbUser("u@example.com", "U", "Ser", "en");
        user.setId(1L);
        user.setOrganizationId(20L);

        Organization real = new Organization();
        real.setId(20L);
        real.setCustomerId("C-abc");
        real.setName("Workspace C-abc");

        when(organizationRepository.findByCustomerId("C-abc")).thenReturn(Optional.of(real));
        when(organizationRepository.save(real)).thenReturn(real);

        organizationService.ensureUserLinkedToOrganization(user, "C-abc", "acme.test");

        assertEquals("acme.test", real.getName());
        verify(organizationRepository).save(real);
    }

    @Test
    void ensureUserLinkedToOrganization_whenCustomerIdUnavailable_leavesUserWithoutOrganization() {
        User user = createDbUser("u@example.com", "U", "Ser", "en");
        user.setId(1L);
        user.setOrganizationId(null);

        organizationService.ensureUserLinkedToOrganization(user, null, null);

        assertNull(user.getOrganizationId());
        verify(userRepository, never()).save(any());
        verify(organizationRepository, never()).save(any());
        verify(organizationRepository, never()).deleteById(any());
        verifyNoInteractions(userSecurityPreferenceRepository);
    }

    @Test
    void getAllPaged_withNullPageToken_startsAtPageZero() {
        Organization org1 = new Organization();
        org1.setName("Alpha");
        Organization org2 = new Organization();
        org2.setName("Bravo");

        // Mock a Page with 2 elements, that is the LAST page (no next page)
        Page<Organization> mockPage = new org.springframework.data.domain.PageImpl<>(
                List.of(org1, org2),
                PageRequest.of(0, 10, Sort.by("name").ascending()),
                2);

        when(organizationRepository.findAllWithSearch(null, PageRequest.of(0, 10, Sort.by("name").ascending())))
                .thenReturn(mockPage);

        var response = organizationService.getAllPaged(null, 10, null);

        assertEquals(2, response.organizations().size());
        assertEquals("Alpha", response.organizations().get(0).getName());
        assertNull(response.nextPageToken(), "Should be null because there is no next page");
    }

    @Test
    void getAllPaged_withEmptyPageToken_startsAtPageZero() {
        Organization org1 = new Organization();

        Page<Organization> mockPage = new org.springframework.data.domain.PageImpl<>(
                List.of(org1),
                PageRequest.of(0, 10, Sort.by("name").ascending()),
                1);

        when(organizationRepository.findAllWithSearch(null, PageRequest.of(0, 10, Sort.by("name").ascending())))
                .thenReturn(mockPage);

        var response = organizationService.getAllPaged("", 10, null);

        assertEquals(1, response.organizations().size());
    }

    @Test
    void getAllPaged_withNextPageToken_fetchesCorrectPage() {
        Organization org1 = new Organization();

        Page<Organization> mockPage = new org.springframework.data.domain.PageImpl<>(
                List.of(org1),
                PageRequest.of(2, 10, Sort.by("name").ascending()),
                40);

        when(organizationRepository.findAllWithSearch("test", PageRequest.of(2, 10, Sort.by("name").ascending())))
                .thenReturn(mockPage);

        var response = organizationService.getAllPaged("2", 10, "test");

        assertEquals(1, response.organizations().size());
        assertEquals("3", response.nextPageToken(), "Should indicate the next page token is 3");
    }

    @Test
    void getAllPaged_noResults_returnsEmptyListAndNullToken() {
        Page<Organization> mockPage = new org.springframework.data.domain.PageImpl<>(
                Collections.emptyList(),
                PageRequest.of(0, 10, Sort.by("name").ascending()),
                0);

        when(organizationRepository.findAllWithSearch("notfound", PageRequest.of(0, 10, Sort.by("name").ascending())))
                .thenReturn(mockPage);

        var response = organizationService.getAllPaged(null, 10, "notfound");

        assertEquals(0, response.organizations().size());
        assertNull(response.nextPageToken());
    }
}
