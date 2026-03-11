package com.cloudmen.cloudguard.scheduler;

import com.cloudmen.cloudguard.service.notification.ResolvedNotificationService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
public class ResolvedNotificationCleanupScheduler {

    private static final Logger log = LoggerFactory.getLogger(ResolvedNotificationCleanupScheduler.class);

    private final ResolvedNotificationService resolvedNotificationService;

    public ResolvedNotificationCleanupScheduler(ResolvedNotificationService resolvedNotificationService) {
        this.resolvedNotificationService = resolvedNotificationService;
    }

    @Scheduled(cron = "0 0 2 * * *") // Run daily at 2 AM
    public void cleanupOldResolvedNotifications() {
        int deleted = resolvedNotificationService.deleteOlderThan30Days();
        if (deleted > 0) {
            log.info("Deleted {} resolved notifications older than 30 days", deleted);
        }
    }
}
