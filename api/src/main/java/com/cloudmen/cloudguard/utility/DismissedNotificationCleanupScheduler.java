package com.cloudmen.cloudguard.utility;

import com.cloudmen.cloudguard.service.notification.DismissedNotificationService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
public class DismissedNotificationCleanupScheduler {

    private static final Logger log = LoggerFactory.getLogger(DismissedNotificationCleanupScheduler.class);

    private final DismissedNotificationService dismissedNotificationService;

    public DismissedNotificationCleanupScheduler(DismissedNotificationService dismissedNotificationService) {
        this.dismissedNotificationService = dismissedNotificationService;
    }

    @Scheduled(cron = "0 0 2 * * *") // Run daily at 2 AM
    public void cleanupOldDismissedNotifications() {
        int deleted = dismissedNotificationService.deleteOlderThan30Days();
        if (deleted > 0) {
            log.info("Deleted {} dismissed notifications older than 30 days", deleted);
        }
    }
}
