package com.cloudmen.cloudguard.unit.service.notification;

import com.cloudmen.cloudguard.configuration.SolvedNotificationCleanupProperties;
import com.cloudmen.cloudguard.domain.model.notification.NotificationInstanceStatus;
import com.cloudmen.cloudguard.repository.NotificationInstanceRepository;
import com.cloudmen.cloudguard.service.notification.SolvedNotificationCleanupService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class SolvedNotificationCleanupServiceTest {

    @Mock
    NotificationInstanceRepository repository;

    SolvedNotificationCleanupProperties properties;
    SolvedNotificationCleanupService service;

    @BeforeEach
    void setUp() {
        properties = new SolvedNotificationCleanupProperties();
        service = new SolvedNotificationCleanupService(properties, repository);
    }

    @Test
    void deleteExpired_returnsZeroWhenDisabled() {
        properties.setEnabled(false);
        assertEquals(0, service.deleteExpiredSolvedNotifications());
        verify(repository, never()).deleteByStatusAndSolvedAtBefore(any(), any());
    }

    @Test
    void deleteExpired_returnsZeroWhenRetentionBelowOne() {
        properties.setRetentionDays(0);
        assertEquals(0, service.deleteExpiredSolvedNotifications());
        verify(repository, never()).deleteByStatusAndSolvedAtBefore(any(), any());
    }

    @Test
    void deleteExpired_callsRepositoryWithSolvedStatusAndCutoffAboutRetentionDaysAgo() {
        properties.setEnabled(true);
        properties.setRetentionDays(30);
        when(repository.deleteByStatusAndSolvedAtBefore(eq(NotificationInstanceStatus.SOLVED), any(LocalDateTime.class)))
                .thenReturn(3);

        int removed = service.deleteExpiredSolvedNotifications();

        assertEquals(3, removed);
        ArgumentCaptor<LocalDateTime> cutoffCaptor = ArgumentCaptor.forClass(LocalDateTime.class);
        verify(repository).deleteByStatusAndSolvedAtBefore(eq(NotificationInstanceStatus.SOLVED), cutoffCaptor.capture());
        LocalDateTime cutoff = cutoffCaptor.getValue();
        long daysBetween = ChronoUnit.DAYS.between(cutoff.toLocalDate(), LocalDateTime.now().toLocalDate());
        assertTrue(daysBetween >= 29 && daysBetween <= 31, "cutoff should be about retention days ago");
    }
}
