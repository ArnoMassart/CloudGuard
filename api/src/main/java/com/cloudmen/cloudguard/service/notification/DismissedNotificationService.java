package com.cloudmen.cloudguard.service.notification;

import com.cloudmen.cloudguard.domain.model.User;
import com.cloudmen.cloudguard.domain.model.notification.NotificationInstance;

import com.cloudmen.cloudguard.domain.model.notification.NotificationSeverity;
import com.cloudmen.cloudguard.dto.notifications.DismissNotificationRequest;
import com.cloudmen.cloudguard.exception.OrganizationRequiredException;
import com.cloudmen.cloudguard.repository.NotificationInstanceRepository;
import com.cloudmen.cloudguard.repository.UserRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Objects;

@Service
public class DismissedNotificationService {

    private static final int RETENTION_DAYS = 30;

    private final NotificationInstanceRepository notificationInstanceRepository;
    private final UserRepository userRepository;

    public DismissedNotificationService(
            NotificationInstanceRepository notificationInstanceRepository, UserRepository userRepository) {
        this.notificationInstanceRepository = notificationInstanceRepository;
        this.userRepository = userRepository;
    }

    @Transactional
    public void markAsDismissed(String userEmail, DismissNotificationRequest request) {
        Long orgId = requireOrganizationId(userEmail);
        NotificationInstance row =
                notificationInstanceRepository
                        .findByOrganizationIdAndSourceAndNotificationType(
                                orgId, request.source(), request.notificationType())
                        .orElseThrow(
                                () -> new IllegalStateException(
                                        "Cannot dismiss: no notification row exists for org "
                                                + orgId + " source=" + request.source()
                                                + " type=" + request.notificationType()));
        applySnapshotFromRequest(row, request);
        row.setDismissedAt(LocalDateTime.now());
        notificationInstanceRepository.save(row);
    }

    public List<NotificationInstance> getDismissedForOrganization(Long organizationId) {
        if (organizationId == null) {
            return List.of();
        }
        return notificationInstanceRepository.findByOrganizationIdAndDismissedAtIsNotNullOrderByDismissedAtDesc(
                organizationId);
    }

    public boolean isDismissed(Long organizationId, String source, String notificationType) {
        if (organizationId == null) {
            return false;
        }
        return notificationInstanceRepository
                .findByOrganizationIdAndSourceAndNotificationType(organizationId, source, notificationType)
                .map(r -> r.getDismissedAt() != null)
                .orElse(false);
    }

    @Transactional
    public boolean unDismiss(String userEmail, String source, String notificationType) {
        Long orgId = requireOrganizationId(userEmail);
        return notificationInstanceRepository
                .findByOrganizationIdAndSourceAndNotificationType(orgId, source, notificationType)
                .filter(r -> r.getDismissedAt() != null)
                .map(
                        r -> {
                            r.setDismissedAt(null);
                            notificationInstanceRepository.save(r);
                            return true;
                        })
                .orElse(false);
    }

    @Transactional
    public int deleteOlderThan30Days() {
        LocalDateTime cutoff = LocalDateTime.now().minusDays(RETENTION_DAYS);
        return notificationInstanceRepository.clearDismissedAtBefore(cutoff);
    }

    private void applySnapshotFromRequest(NotificationInstance row, DismissNotificationRequest request) {
        row.setSourceLabel(request.sourceLabel());
        row.setSourceRoute(request.sourceRoute());
        row.setTitle(request.title());
        row.setDescription(request.description());
        if (request.severity() != null && !request.severity().isBlank()) {
            row.setSeverity(NotificationSeverity.fromDtoString(request.severity()));
        } else if (row.getSeverity() == null) {
            row.setSeverity(NotificationSeverity.MEDIUM);
        }
        row.setRecommendedActions(
                request.recommendedActions() != null ? request.recommendedActions() : List.of());
    }

    private Long requireOrganizationId(String userEmail) {
        return userRepository
                .findByEmail(userEmail)
                .map(User::getOrganizationId)
                .filter(Objects::nonNull)
                .orElseThrow(
                        () ->
                                new OrganizationRequiredException(
                                        "Organization is required to dismiss notifications for your workspace."));
    }
}
