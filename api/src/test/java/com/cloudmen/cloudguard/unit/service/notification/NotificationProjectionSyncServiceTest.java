package com.cloudmen.cloudguard.unit.service.notification;

import com.cloudmen.cloudguard.domain.model.User;
import com.cloudmen.cloudguard.domain.model.UserRole;
import com.cloudmen.cloudguard.domain.model.notification.NotificationInstance;
import com.cloudmen.cloudguard.domain.model.notification.NotificationInstanceStatus;
import com.cloudmen.cloudguard.domain.model.notification.NotificationSeverity;
import com.cloudmen.cloudguard.dto.notifications.NotificationDto;
import com.cloudmen.cloudguard.repository.NotificationInstanceRepository;
import com.cloudmen.cloudguard.repository.UserRepository;
import com.cloudmen.cloudguard.service.notification.NotificationAggregationService;
import com.cloudmen.cloudguard.service.notification.NotificationProjectionSyncService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Locale;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class NotificationProjectionSyncServiceTest {

    private static final long ORG_ID = 42L;

    @Mock
    private NotificationInstanceRepository instanceRepository;
    @Mock
    private NotificationAggregationService aggregationService;
    @Mock
    private UserRepository userRepository;

    private NotificationProjectionSyncService syncService;

    @BeforeEach
    void setUp() {
        syncService = new NotificationProjectionSyncService(instanceRepository, aggregationService, userRepository);
    }

    @Test
    void syncOrganization_insertsNewRow_whenSnapshotHasFinding() {
        User actor = new User();
        actor.setId(1L);
        actor.setEmail("admin@example.com");
        actor.setLanguage("en");
        when(userRepository.findByOrganizationIdAndRoleOrderByIdAsc(ORG_ID, UserRole.SUPER_ADMIN))
                .thenReturn(List.of(actor));

        NotificationDto dto =
                new NotificationDto(
                        "n-1",
                        "critical",
                        "t",
                        "d",
                        List.of("a"),
                        "user-control",
                        "users-groups",
                        "Users",
                        "/users-groups",
                        false,
                        true);
        when(aggregationService.buildActiveSnapshot("admin@example.com", Locale.ENGLISH)).thenReturn(List.of(dto));

        when(instanceRepository.findByOrganizationId(ORG_ID)).thenReturn(List.of());

        syncService.syncOrganization(ORG_ID);

        ArgumentCaptor<NotificationInstance> captor = ArgumentCaptor.forClass(NotificationInstance.class);
        verify(instanceRepository).save(captor.capture());
        NotificationInstance saved = captor.getValue();
        assertEquals(ORG_ID, saved.getOrganizationId());
        assertEquals("users-groups", saved.getSource());
        assertEquals("user-control", saved.getNotificationType());
        assertSame(NotificationInstanceStatus.ACTIVE, saved.getStatus());
        assertEquals(NotificationSeverity.CRITICAL, saved.getSeverity());
        assertEquals("t", saved.getTitle());
    }

    @Test
    void syncOrganization_marksSolved_whenNoLongerInSnapshot() {
        User actor = new User();
        actor.setId(1L);
        actor.setEmail("admin@example.com");
        actor.setLanguage("en");
        when(userRepository.findByOrganizationIdAndRoleOrderByIdAsc(ORG_ID, UserRole.SUPER_ADMIN))
                .thenReturn(List.of(actor));
        when(aggregationService.buildActiveSnapshot("admin@example.com", Locale.ENGLISH)).thenReturn(List.of());

        NotificationInstance open = new NotificationInstance();
        open.setId(9L);
        open.setOrganizationId(ORG_ID);
        open.setSource("users-groups");
        open.setNotificationType("user-control");
        open.setStatus(NotificationInstanceStatus.ACTIVE);
        open.setSeverity(NotificationSeverity.CRITICAL);

        when(instanceRepository.findByOrganizationId(ORG_ID)).thenReturn(List.of(open));

        syncService.syncOrganization(ORG_ID);

        verify(instanceRepository).save(open);
        assertSame(NotificationInstanceStatus.SOLVED, open.getStatus());
    }

    @Test
    void syncOrganization_skipsWhenNoSuperAdmin() {
        when(userRepository.findByOrganizationIdAndRoleOrderByIdAsc(ORG_ID, UserRole.SUPER_ADMIN))
                .thenReturn(List.of());

        syncService.syncOrganization(ORG_ID);

        verifyNoInteractions(aggregationService);
        verify(instanceRepository, never()).save(any());
    }
}
