package com.cloudmen.cloudguard.service.notification;

import com.cloudmen.cloudguard.configuration.NotificationProjectionProperties;
import com.cloudmen.cloudguard.repository.UserRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class NotificationProjectionScheduler {

    private static final Logger log = LoggerFactory.getLogger(NotificationProjectionScheduler.class);

    private final NotificationProjectionProperties properties;
    private final UserRepository userRepository;
    private final NotificationProjectionSyncService syncService;

    public NotificationProjectionScheduler(
            NotificationProjectionProperties properties,
            UserRepository userRepository,
            NotificationProjectionSyncService syncService) {
        this.properties = properties;
        this.userRepository = userRepository;
        this.syncService = syncService;
    }

    @Scheduled(cron = "${cloudguard.notifications.projection.sync-cron:0 0/10 * * * *}")
    public void syncAllOrganizations() {
        if (!properties.isSyncEnabled()) {
            return;
        }
        List<Long> orgIds = userRepository.findDistinctOrganizationIds();
        for (Long orgId : orgIds) {
            try {
                syncService.syncOrganization(orgId);
            } catch (Exception e) {
                log.warn("Notification projection sync failed for organization {}: {}", orgId, e.getMessage());
            }
        }
    }
}
