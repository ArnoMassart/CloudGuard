package com.cloudmen.cloudguard.unit.service.notification;

import com.cloudmen.cloudguard.domain.model.User;
import com.cloudmen.cloudguard.domain.model.UserRole;
import com.cloudmen.cloudguard.domain.model.notification.NotificationInstance;
import com.cloudmen.cloudguard.domain.model.notification.NotificationInstanceStatus;
import com.cloudmen.cloudguard.domain.model.notification.NotificationSeverity;
import com.cloudmen.cloudguard.repository.NotificationInstanceRepository;
import com.cloudmen.cloudguard.repository.UserRepository;
import com.cloudmen.cloudguard.service.reminder.CriticalNotificationReminderEmailService;
import com.cloudmen.cloudguard.service.reminder.CriticalNotificationWeeklyReminderService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Locale;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.same;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CriticalNotificationWeeklyReminderServiceTest {

    private static final long ORG = 5L;

    @Mock
    private NotificationInstanceRepository notificationInstanceRepository;

    @Mock
    private UserRepository userRepository;

    @Mock
    private CriticalNotificationReminderEmailService reminderEmailService;

    private CriticalNotificationWeeklyReminderService service;

    @BeforeEach
    void setUp() {
        service =
                new CriticalNotificationWeeklyReminderService(
                        notificationInstanceRepository, userRepository, reminderEmailService);
    }

    @Test
    void sendWeeklyReminders_skipsWhenNoCriticalRows() {
        when(userRepository.findDistinctOrganizationIds()).thenReturn(List.of(ORG));
        when(notificationInstanceRepository.findByOrganizationIdAndStatusAndSeverity(
                        eq(ORG), eq(NotificationInstanceStatus.ACTIVE), eq(NotificationSeverity.CRITICAL)))
                .thenReturn(List.of());

        service.sendWeeklyRemindersForAllOrganizations();

        verify(userRepository, never()).findByOrganizationIdAndRoleOrderByIdAsc(any(), any());
        verify(reminderEmailService, never()).sendWeeklyCriticalDigest(any(), any(), any());
    }

    @Test
    void sendWeeklyReminders_skipsEmailWhenNoSuperAdmins() {
        when(userRepository.findDistinctOrganizationIds()).thenReturn(List.of(ORG));
        NotificationInstance crit = new NotificationInstance();
        crit.setTitle("Gap");
        when(notificationInstanceRepository.findByOrganizationIdAndStatusAndSeverity(
                        eq(ORG), eq(NotificationInstanceStatus.ACTIVE), eq(NotificationSeverity.CRITICAL)))
                .thenReturn(List.of(crit));
        when(userRepository.findByOrganizationIdAndRoleOrderByIdAsc(ORG, UserRole.SUPER_ADMIN))
                .thenReturn(List.of());

        service.sendWeeklyRemindersForAllOrganizations();

        verify(reminderEmailService, never()).sendWeeklyCriticalDigest(any(), any(), any());
    }

    @Test
    void sendWeeklyReminders_sendsToEachSuperAdminWithEmail() {
        when(userRepository.findDistinctOrganizationIds()).thenReturn(List.of(ORG));
        NotificationInstance crit = new NotificationInstance();
        crit.setTitle("2FA");
        crit.setSourceLabel("Users");
        List<NotificationInstance> criticalList = List.of(crit);
        when(notificationInstanceRepository.findByOrganizationIdAndStatusAndSeverity(
                        eq(ORG), eq(NotificationInstanceStatus.ACTIVE), eq(NotificationSeverity.CRITICAL)))
                .thenReturn(criticalList);

        User a1 = new User();
        a1.setEmail("a1@example.com");
        a1.setLanguage("en");
        User a2 = new User();
        a2.setEmail("a2@example.com");
        a2.setLanguage("nl");
        User noEmail = new User();
        noEmail.setEmail(" ");
        when(userRepository.findByOrganizationIdAndRoleOrderByIdAsc(ORG, UserRole.SUPER_ADMIN))
                .thenReturn(List.of(a1, noEmail, a2));
        when(reminderEmailService.sendWeeklyCriticalDigest(any(), any(), any())).thenReturn(true);

        service.sendWeeklyRemindersForAllOrganizations();

        verify(reminderEmailService)
                .sendWeeklyCriticalDigest(eq("a1@example.com"), eq(Locale.forLanguageTag("en")), same(criticalList));
        verify(reminderEmailService).sendWeeklyCriticalDigest(eq("a2@example.com"), eq(Locale.forLanguageTag("nl")), same(criticalList));
        verify(reminderEmailService, never()).sendWeeklyCriticalDigest(eq(" "), any(), any());
    }

    @Test
    void sendWeeklyReminders_usesNlWhenLanguageMissing() {
        when(userRepository.findDistinctOrganizationIds()).thenReturn(List.of(ORG));
        List<NotificationInstance> criticalList = List.of(new NotificationInstance());
        when(notificationInstanceRepository.findByOrganizationIdAndStatusAndSeverity(
                        eq(ORG), eq(NotificationInstanceStatus.ACTIVE), eq(NotificationSeverity.CRITICAL)))
                .thenReturn(criticalList);
        User admin = new User();
        admin.setEmail("x@y.com");
        admin.setLanguage(null);
        when(userRepository.findByOrganizationIdAndRoleOrderByIdAsc(ORG, UserRole.SUPER_ADMIN))
                .thenReturn(List.of(admin));
        when(reminderEmailService.sendWeeklyCriticalDigest(any(), any(), any())).thenReturn(true);

        service.sendWeeklyRemindersForAllOrganizations();

        verify(reminderEmailService)
                .sendWeeklyCriticalDigest(eq("x@y.com"), eq(Locale.forLanguageTag("nl")), same(criticalList));
    }

    @Test
    void sendWeeklyReminders_continuesWhenOneOrgThrows() {
        when(userRepository.findDistinctOrganizationIds()).thenReturn(List.of(1L, 2L));
        when(notificationInstanceRepository.findByOrganizationIdAndStatusAndSeverity(
                        eq(1L), eq(NotificationInstanceStatus.ACTIVE), eq(NotificationSeverity.CRITICAL)))
                .thenThrow(new RuntimeException("db"));
        List<NotificationInstance> criticalList = List.of(new NotificationInstance());
        when(notificationInstanceRepository.findByOrganizationIdAndStatusAndSeverity(
                        eq(2L), eq(NotificationInstanceStatus.ACTIVE), eq(NotificationSeverity.CRITICAL)))
                .thenReturn(criticalList);
        User admin = new User();
        admin.setEmail("ok@example.com");
        admin.setLanguage("en");
        when(userRepository.findByOrganizationIdAndRoleOrderByIdAsc(2L, UserRole.SUPER_ADMIN))
                .thenReturn(List.of(admin));
        when(reminderEmailService.sendWeeklyCriticalDigest(any(), any(), any())).thenReturn(true);

        service.sendWeeklyRemindersForAllOrganizations();

        verify(reminderEmailService).sendWeeklyCriticalDigest(eq("ok@example.com"), any(), same(criticalList));
    }
}
