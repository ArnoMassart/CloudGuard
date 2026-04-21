package com.cloudmen.cloudguard.service.notification;

import com.cloudmen.cloudguard.configuration.SolvedNotificationCleanupProperties;
import com.cloudmen.cloudguard.domain.model.notification.NotificationInstanceStatus;
import com.cloudmen.cloudguard.repository.NotificationInstanceRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

@Service
public class SolvedNotificationCleanupService {

    private static final Logger log = LoggerFactory.getLogger(SolvedNotificationCleanupService.class);

    private final SolvedNotificationCleanupProperties properties;
    private final NotificationInstanceRepository notificationInstanceRepository;

    public SolvedNotificationCleanupService(
            SolvedNotificationCleanupProperties properties,
            NotificationInstanceRepository notificationInstanceRepository) {
        this.properties = properties;
        this.notificationInstanceRepository = notificationInstanceRepository;
    }

    /**
     * Removes persisted SOLVED notification rows whose {@code solved_at} is older than the configured retention.
     */
    @Transactional
    public int deleteExpiredSolvedNotifications() {
        if (!properties.isEnabled() || properties.getRetentionDays() < 1) {
            return 0;
        }
        LocalDateTime cutoff = LocalDateTime.now().minusDays(properties.getRetentionDays());
        int removed =
                notificationInstanceRepository.deleteByStatusAndSolvedAtBefore(
                        NotificationInstanceStatus.SOLVED, cutoff);
        if (removed > 0) {
            log.info(
                    "Removed {} solved notification projection row(s) older than {} day(s) (before {})",
                    removed,
                    properties.getRetentionDays(),
                    cutoff);
        }
        return removed;
    }
}
