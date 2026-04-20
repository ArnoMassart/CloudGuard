package com.cloudmen.cloudguard.service.reminder;

import com.cloudmen.cloudguard.domain.model.User;
import com.cloudmen.cloudguard.domain.model.UserRole;
import com.cloudmen.cloudguard.domain.model.notification.NotificationInstance;
import com.cloudmen.cloudguard.domain.model.notification.NotificationInstanceStatus;
import com.cloudmen.cloudguard.domain.model.notification.NotificationSeverity;
import com.cloudmen.cloudguard.repository.NotificationInstanceRepository;
import com.cloudmen.cloudguard.repository.UserRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Locale;

@Service
public class CriticalNotificationWeeklyReminderService {

    private static final Logger log = LoggerFactory.getLogger(CriticalNotificationWeeklyReminderService.class);

    private final NotificationInstanceRepository notificationInstanceRepository;
    private final UserRepository userRepository;
    private final CriticalNotificationReminderEmailService reminderEmailService;

    public CriticalNotificationWeeklyReminderService(
            NotificationInstanceRepository notificationInstanceRepository,
            UserRepository userRepository,
            CriticalNotificationReminderEmailService reminderEmailService) {
        this.notificationInstanceRepository = notificationInstanceRepository;
        this.userRepository = userRepository;
        this.reminderEmailService = reminderEmailService;
    }

    public void sendWeeklyRemindersForAllOrganizations() {
        List<Long> orgIds = userRepository.findDistinctOrganizationIds();
        for (Long orgId : orgIds) {
            try {
                processOrganization(orgId);
            } catch (Exception e) {
                log.warn("Weekly critical notification reminder failed for organization {}: {}", orgId, e.getMessage());
            }
        }
    }

    private void processOrganization(Long organizationId) {
        List<NotificationInstance> critical =
                notificationInstanceRepository.findByOrganizationIdAndStatusAndSeverity(
                        organizationId, NotificationInstanceStatus.ACTIVE, NotificationSeverity.CRITICAL);
        if (critical.isEmpty()) {
            return;
        }
        List<User> admins =
                userRepository.findByOrganizationIdAndRoleOrderByIdAsc(organizationId, UserRole.SUPER_ADMIN);
        if (admins.isEmpty()) {
            log.debug(
                    "Skipping weekly critical reminder for organization {}: no SUPER_ADMIN recipients",
                    organizationId);
            return;
        }
        for (User admin : admins) {
            if (admin.getEmail() == null || admin.getEmail().isBlank()) {
                continue;
            }
            Locale locale = resolveLocale(admin);
            boolean sent =
                    reminderEmailService.sendWeeklyCriticalDigest(admin.getEmail(), locale, critical);
            if (!sent) {
                log.warn(
                        "Weekly critical digest could not be sent to {} (organization {})",
                        admin.getEmail(),
                        organizationId);
            }
        }
    }

    private static Locale resolveLocale(User admin) {
        String lang = admin.getLanguage();
        if (lang == null || lang.isBlank()) {
            return Locale.forLanguageTag("nl");
        }
        return Locale.forLanguageTag(lang.replace('_', '-'));
    }
}
