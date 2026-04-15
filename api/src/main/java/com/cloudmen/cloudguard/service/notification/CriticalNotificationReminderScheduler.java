package com.cloudmen.cloudguard.service.notification;

import com.cloudmen.cloudguard.configuration.NotificationReminderProperties;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
public class CriticalNotificationReminderScheduler {

    private final NotificationReminderProperties properties;
    private final CriticalNotificationWeeklyReminderService reminderService;

    public CriticalNotificationReminderScheduler(
            NotificationReminderProperties properties,
            CriticalNotificationWeeklyReminderService reminderService) {
        this.properties = properties;
        this.reminderService = reminderService;
    }

    @Scheduled(cron = "${cloudguard.notifications.reminder.weekly-critical-cron:0 0 9 * * MON}")
    public void sendWeeklyCriticalDigests() {
        if (!properties.isWeeklyCriticalEnabled()) {
            return;
        }
        reminderService.sendWeeklyRemindersForAllOrganizations();
    }
}
