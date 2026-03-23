package com.cloudmen.cloudguard.service.notification;

import com.cloudmen.cloudguard.domain.model.feedback.DismissedNotification;
import com.cloudmen.cloudguard.dto.notifications.DismissNotificationRequest;
import com.cloudmen.cloudguard.repository.DismissedNotificationRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Service
public class DismissedNotificationService {

    private static final int RETENTION_DAYS = 30;

    private final DismissedNotificationRepository repository;

    public DismissedNotificationService(DismissedNotificationRepository repository) {
        this.repository = repository;
    }

    @Transactional
    public DismissedNotification markAsDismissed(String userId, DismissNotificationRequest request) {
        return repository.findByUserIdAndSourceAndNotificationType(userId, request.source(), request.notificationType())
                .map(existing -> existing)
                .orElseGet(() -> {
                    DismissedNotification d = new DismissedNotification();
                    d.setUserId(userId);
                    d.setSource(request.source());
                    d.setNotificationType(request.notificationType());
                    d.setSourceLabel(request.sourceLabel());
                    d.setSourceRoute(request.sourceRoute());
                    d.setTitle(request.title());
                    d.setDescription(request.description());
                    d.setSeverity(request.severity());
                    d.setRecommendedActions(request.recommendedActions() != null ? request.recommendedActions() : List.of());
                    d.setDismissedAt(LocalDateTime.now());
                    return repository.save(d);
                });
    }

    public List<DismissedNotification> getDismissedForUser(String userId) {
        return repository.findByUserIdOrderByDismissedAtDesc(userId);
    }

    public boolean isDismissed(String userId, String source, String notificationType) {
        return repository.existsByUserIdAndSourceAndNotificationType(userId, source, notificationType);
    }

    @Transactional
    public boolean unDismiss(String userId, String source, String notificationType) {
        int deleted = repository.deleteByUserIdAndSourceAndNotificationType(userId, source, notificationType);
        return deleted > 0;
    }

    @Transactional
    public int deleteOlderThan30Days() {
        LocalDateTime cutoff = LocalDateTime.now().minusDays(RETENTION_DAYS);
        return repository.deleteByDismissedAtBefore(cutoff);
    }
}
