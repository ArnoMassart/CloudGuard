package com.cloudmen.cloudguard.unit.service.notification;

import com.cloudmen.cloudguard.domain.model.User;
import com.cloudmen.cloudguard.domain.model.notification.NotificationInstance;
import com.cloudmen.cloudguard.domain.model.notification.NotificationInstanceStatus;
import com.cloudmen.cloudguard.domain.model.notification.NotificationSeverity;
import com.cloudmen.cloudguard.dto.notifications.DismissNotificationRequest;
import com.cloudmen.cloudguard.exception.OrganizationRequiredException;
import com.cloudmen.cloudguard.repository.NotificationInstanceRepository;
import com.cloudmen.cloudguard.repository.UserRepository;
import com.cloudmen.cloudguard.service.notification.DismissedNotificationService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class DismissedNotificationServiceTest {

    private static final long ORG_ID = 7L;

    @Mock
    NotificationInstanceRepository notificationInstanceRepository;

    @Mock
    UserRepository userRepository;

    DismissedNotificationService service;

    @BeforeEach
    void setUp() {
        service = new DismissedNotificationService(notificationInstanceRepository, userRepository);
    }

    @Test
    void markAsDismissed_createsAndSavesWhenNotExisting() {
        User u = new User();
        u.setOrganizationId(ORG_ID);
        when(userRepository.findByEmail("admin@x.com")).thenReturn(Optional.of(u));
        when(notificationInstanceRepository.findByOrganizationIdAndSourceAndNotificationType(ORG_ID, "src", "typ"))
                .thenReturn(Optional.empty());

        DismissNotificationRequest req =
                new DismissNotificationRequest("src", "typ", "label", "/route", "t", "d", "warning", List.of("a"));

        service.markAsDismissed("admin@x.com", req);

        ArgumentCaptor<NotificationInstance> cap = ArgumentCaptor.forClass(NotificationInstance.class);
        verify(notificationInstanceRepository).save(cap.capture());
        assertEquals(ORG_ID, cap.getValue().getOrganizationId());
        assertEquals("src", cap.getValue().getSource());
        assertEquals("typ", cap.getValue().getNotificationType());
        assertNotNull(cap.getValue().getDismissedAt());
        assertEquals(NotificationInstanceStatus.ACTIVE, cap.getValue().getStatus());
    }

    @Test
    void markAsDismissed_updatesDismissedAtWhenRowExists() {
        User u = new User();
        u.setOrganizationId(ORG_ID);
        when(userRepository.findByEmail("admin@x.com")).thenReturn(Optional.of(u));
        NotificationInstance existing = new NotificationInstance();
        existing.setId(3L);
        existing.setOrganizationId(ORG_ID);
        existing.setSource("src");
        existing.setNotificationType("typ");
        existing.setSeverity(NotificationSeverity.CRITICAL);
        existing.setStatus(NotificationInstanceStatus.ACTIVE);
        when(notificationInstanceRepository.findByOrganizationIdAndSourceAndNotificationType(ORG_ID, "src", "typ"))
                .thenReturn(Optional.of(existing));

        DismissNotificationRequest req =
                new DismissNotificationRequest("src", "typ", "l", "/", "t2", "d2", "info", List.of());

        service.markAsDismissed("admin@x.com", req);

        verify(notificationInstanceRepository).save(existing);
        assertNotNull(existing.getDismissedAt());
 assertEquals("t2", existing.getTitle());
    }

    @Test
    void markAsDismissed_withoutOrganization_throws() {
        User u = new User();
        u.setOrganizationId(null);
        when(userRepository.findByEmail("u@x.com")).thenReturn(Optional.of(u));
        DismissNotificationRequest req =
                new DismissNotificationRequest("s", "t", "l", "/", "a", "b", "warning", List.of());
        assertThrows(OrganizationRequiredException.class, () -> service.markAsDismissed("u@x.com", req));
        verifyNoInteractions(notificationInstanceRepository);
    }

    @Test
    void getDismissedForOrganization_emptyWhenNullOrg() {
        assertTrue(service.getDismissedForOrganization(null).isEmpty());
        verifyNoInteractions(notificationInstanceRepository);
    }

    @Test
    void getDismissedForOrganization_delegatesToRepository() {
        when(notificationInstanceRepository.findByOrganizationIdAndDismissedAtIsNotNullOrderByDismissedAtDesc(ORG_ID))
                .thenReturn(List.of(new NotificationInstance()));
        assertEquals(1, service.getDismissedForOrganization(ORG_ID).size());
    }

    @Test
    void isDismissed_falseWhenNoOrg() {
        assertFalse(service.isDismissed(null, "s", "t"));
    }

    @Test
    void isDismissed_trueWhenDismissedAtSet() {
        NotificationInstance row = new NotificationInstance();
        row.setDismissedAt(java.time.LocalDateTime.now());
        when(notificationInstanceRepository.findByOrganizationIdAndSourceAndNotificationType(ORG_ID, "s", "t"))
                .thenReturn(Optional.of(row));
        assertTrue(service.isDismissed(ORG_ID, "s", "t"));
    }

    @Test
    void unDismiss_trueWhenCleared() {
        User u = new User();
        u.setOrganizationId(ORG_ID);
        when(userRepository.findByEmail("a@b.com")).thenReturn(Optional.of(u));
        NotificationInstance row = new NotificationInstance();
        row.setDismissedAt(java.time.LocalDateTime.now());
        when(notificationInstanceRepository.findByOrganizationIdAndSourceAndNotificationType(ORG_ID, "s", "t"))
                .thenReturn(Optional.of(row));
        assertTrue(service.unDismiss("a@b.com", "s", "t"));
        assertNull(row.getDismissedAt());
        verify(notificationInstanceRepository).save(row);
    }

    @Test
    void unDismiss_falseWhenNotDismissed() {
        User u = new User();
        u.setOrganizationId(ORG_ID);
        when(userRepository.findByEmail("a@b.com")).thenReturn(Optional.of(u));
        NotificationInstance row = new NotificationInstance();
        when(notificationInstanceRepository.findByOrganizationIdAndSourceAndNotificationType(ORG_ID, "s", "t"))
                .thenReturn(Optional.of(row));
        assertFalse(service.unDismiss("a@b.com", "s", "t"));
        verify(notificationInstanceRepository, never()).save(any());
    }

    @Test
    void deleteOlderThan30Days_returnsUpdateCount() {
        when(notificationInstanceRepository.clearDismissedAtBefore(any())).thenReturn(3);
        assertEquals(3, service.deleteOlderThan30Days());
    }
}
