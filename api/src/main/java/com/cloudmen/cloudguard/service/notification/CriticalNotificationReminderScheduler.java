package com.cloudmen.cloudguard.service.notification;

import com.cloudmen.cloudguard.configuration.NotificationReminderProperties;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Component
public class CriticalNotificationReminderScheduler {

    private final NotificationReminderProperties properties;
    private final CriticalNotificationWeeklyReminderService reminderService;

    private static final Logger log = LoggerFactory.getLogger(CriticalNotificationReminderScheduler.class);

    public CriticalNotificationReminderScheduler(
            NotificationReminderProperties properties,
            CriticalNotificationWeeklyReminderService reminderService) {
        this.properties = properties;
        this.reminderService = reminderService;
    }

    @Scheduled(cron = "${cloudguard.notifications.reminder.weekly-critical-cron:0 * * * * *}")
    public void sendWeeklyCriticalDigests() {
        if (!properties.isWeeklyCriticalEnabled()) {
            log.debug("Weekly critical reminder skipped: feature disabled");
            return;
        }
        log.info("Weekly critical reminder job started");
        reminderService.sendWeeklyRemindersForAllOrganizations();
    }
}
