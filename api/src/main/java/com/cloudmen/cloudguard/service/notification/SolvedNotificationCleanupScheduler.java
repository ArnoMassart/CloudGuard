package com.cloudmen.cloudguard.service.notification;

import com.cloudmen.cloudguard.configuration.SolvedNotificationCleanupProperties;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
public class SolvedNotificationCleanupScheduler {

    private static final Logger log = LoggerFactory.getLogger(SolvedNotificationCleanupScheduler.class);

    private final SolvedNotificationCleanupProperties properties;
    private final SolvedNotificationCleanupService cleanupService;

    public SolvedNotificationCleanupScheduler(
            SolvedNotificationCleanupProperties properties, SolvedNotificationCleanupService cleanupService) {
        this.properties = properties;
        this.cleanupService = cleanupService;
    }

    @Scheduled(cron = "${cloudguard.notifications.solved-cleanup.cron:0 0 4 * * *}")
    public void runCleanup() {
        if (!properties.isEnabled()) {
            return;
        }
        try {
            cleanupService.deleteExpiredSolvedNotifications();
        } catch (Exception e) {
            log.warn("Solved notification cleanup failed: {}", e.getMessage());
        }
    }
}
