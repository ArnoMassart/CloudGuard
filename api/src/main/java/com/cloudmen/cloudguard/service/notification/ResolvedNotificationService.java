package com.cloudmen.cloudguard.service.notification;

import com.cloudmen.cloudguard.domain.model.feedback.ResolvedNotification;
import com.cloudmen.cloudguard.dto.notifications.ResolveNotificationRequest;
import com.cloudmen.cloudguard.repository.ResolvedNotificationRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Service
public class ResolvedNotificationService {

    private static final int RETENTION_DAYS = 30;

    private final ResolvedNotificationRepository repository;

    public ResolvedNotificationService(ResolvedNotificationRepository repository) {
        this.repository = repository;
    }

    @Transactional
    public ResolvedNotification markAsResolved(String userId, ResolveNotificationRequest request) {
        return repository.findByUserIdAndSourceAndNotificationType(userId, request.source(), request.notificationType())
                .map(existing -> existing) // Already resolved
                .orElseGet(() -> {
                    ResolvedNotification r = new ResolvedNotification();
                    r.setUserId(userId);
                    r.setSource(request.source());
                    r.setNotificationType(request.notificationType());
                    r.setSourceLabel(request.sourceLabel());
                    r.setSourceRoute(request.sourceRoute());
                    r.setTitle(request.title());
                    r.setDescription(request.description());
                    r.setSeverity(request.severity());
                    r.setRecommendedActions(request.recommendedActions() != null ? request.recommendedActions() : List.of());
                    r.setResolvedAt(LocalDateTime.now());
                    return repository.save(r);
                });
    }

    public List<ResolvedNotification> getResolvedForUser(String userId) {
        return repository.findByUserIdOrderByResolvedAtDesc(userId);
    }

    public boolean isResolved(String userId, String source, String notificationType) {
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
        return repository.deleteByResolvedAtBefore(cutoff);
    }
}
