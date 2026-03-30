package com.cloudmen.cloudguard.service.notification;

import com.cloudmen.cloudguard.domain.model.feedback.DismissedNotification;
import com.cloudmen.cloudguard.dto.notifications.DismissNotificationRequest;
import com.cloudmen.cloudguard.repository.DismissedNotificationRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class DismissedNotificationServiceTest {

    @Mock
    DismissedNotificationRepository repository;

    DismissedNotificationService service;

    @BeforeEach
    void setUp() {
        service = new DismissedNotificationService(repository);
    }

    @Test
    void markAsDismissed_createsAndSavesWhenNotExisting() {
        when(repository.findByUserIdAndSourceAndNotificationType("u1", "src", "typ"))
                .thenReturn(Optional.empty());

        DismissNotificationRequest req = new DismissNotificationRequest(
                "src", "typ", "label", "/route", "t", "d", "warning", List.of("a"));
        DismissedNotification saved = new DismissedNotification();
        saved.setId(1L);
        when(repository.save(any(DismissedNotification.class))).thenReturn(saved);

        DismissedNotification result = service.markAsDismissed("u1", req);

        assertSame(saved, result);
        ArgumentCaptor<DismissedNotification> cap = ArgumentCaptor.forClass(DismissedNotification.class);
        verify(repository).save(cap.capture());
        assertEquals("u1", cap.getValue().getUserId());
        assertEquals("src", cap.getValue().getSource());
        assertEquals("typ", cap.getValue().getNotificationType());
    }

    @Test
    void markAsDismissed_returnsExistingWithoutSaveWhenAlreadyDismissed() {
        DismissedNotification existing = new DismissedNotification();
        existing.setId(9L);
        when(repository.findByUserIdAndSourceAndNotificationType("u1", "src", "typ"))
                .thenReturn(Optional.of(existing));

        DismissNotificationRequest req = new DismissNotificationRequest(
                "src", "typ", "l", "/", "t", "d", "info", List.of());

        DismissedNotification result = service.markAsDismissed("u1", req);

        assertSame(existing, result);
        verify(repository, never()).save(any());
    }

    @Test
    void getDismissedForUser_delegatesToRepository() {
        when(repository.findByUserIdOrderByDismissedAtDesc("u1")).thenReturn(List.of(new DismissedNotification()));

        assertEquals(1, service.getDismissedForUser("u1").size());
    }

    @Test
    void isDismissed_delegatesToRepository() {
        when(repository.existsByUserIdAndSourceAndNotificationType("u1", "s", "t")).thenReturn(true);
        assertTrue(service.isDismissed("u1", "s", "t"));
    }

    @Test
    void unDismiss_trueWhenRowDeleted() {
        when(repository.deleteByUserIdAndSourceAndNotificationType("u1", "s", "t")).thenReturn(1);
        assertTrue(service.unDismiss("u1", "s", "t"));
    }

    @Test
    void unDismiss_falseWhenNothingDeleted() {
        when(repository.deleteByUserIdAndSourceAndNotificationType("u1", "s", "t")).thenReturn(0);
        assertFalse(service.unDismiss("u1", "s", "t"));
    }

    @Test
    void deleteOlderThan30Days_returnsDeleteCount() {
        when(repository.deleteByDismissedAtBefore(any())).thenReturn(3);
        assertEquals(3, service.deleteOlderThan30Days());
    }
}
