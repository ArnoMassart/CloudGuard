package com.cloudmen.cloudguard.service;

import com.cloudmen.cloudguard.domain.notifications.Notification;
import com.cloudmen.cloudguard.domain.notifications.NotificationSeverity;
import com.cloudmen.cloudguard.domain.notifications.NotificationStatus;
import com.cloudmen.cloudguard.domain.notifications.NotificationUserState;
import com.cloudmen.cloudguard.dto.notifications.CreateNotificationRequest;
import com.cloudmen.cloudguard.dto.notifications.NotificationResponseDto;
import com.cloudmen.cloudguard.dto.notifications.NotificationUserStateDto;
import com.cloudmen.cloudguard.repository.NotificationRepository;
import com.cloudmen.cloudguard.repository.NotificationUserStateRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class NotificationService {

    private final NotificationRepository notificationRepository;
    private final NotificationUserStateRepository notificationUserStateRepository;

    public NotificationService(NotificationRepository notificationRepository,
                              NotificationUserStateRepository notificationUserStateRepository) {
        this.notificationRepository = notificationRepository;
        this.notificationUserStateRepository = notificationUserStateRepository;
    }

    /**
     * Create a new notification. If a notification with the same domainId and notificationType
     * already exists, returns the existing one instead of creating a duplicate.
     */
    @Transactional
    public Notification createNotification(CreateNotificationRequest request) {
        return notificationRepository.findByDomainIdAndNotificationType(request.domainId(), request.notificationType())
                .orElseGet(() -> {
                    Notification notification = new Notification();
                    notification.setDomainId(request.domainId());
                    notification.setTitle(request.title());
                    notification.setMessage(request.message());
                    notification.setRecommendedAction(request.recommendedAction());
                    notification.setSeverity(request.severity());
                    notification.setStatus(NotificationStatus.OPEN);
                    notification.setNotificationType(request.notificationType());
                    notification.setTicketId(request.ticketId());
                    notification.setCreatedAt(LocalDateTime.now());
                    return notificationRepository.save(notification);
                });
    }

    /**
     * Find existing notification by domainId and notificationType, or create a new one.
     */
    @Transactional
    public Notification findOrCreate(CreateNotificationRequest request) {
        return notificationRepository.findByDomainIdAndNotificationType(request.domainId(), request.notificationType())
                .orElseGet(() -> {
                    Notification notification = new Notification();
                    notification.setDomainId(request.domainId());
                    notification.setTitle(request.title());
                    notification.setMessage(request.message());
                    notification.setRecommendedAction(request.recommendedAction());
                    notification.setSeverity(request.severity());
                    notification.setStatus(NotificationStatus.OPEN);
                    notification.setNotificationType(request.notificationType());
                    notification.setTicketId(request.ticketId());
                    notification.setCreatedAt(LocalDateTime.now());
                    return notificationRepository.save(notification);
                });
    }

    /**
     * Get notification by id.
     */
    public Notification getById(Long id) {
        return notificationRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Notification not found: " + id));
    }

    /**
     * Get all notifications for a domain.
     */
    public List<Notification> getByDomainId(String domainId) {
        return notificationRepository.findByDomainId(domainId);
    }

    /**
     * Get all notifications with optional status filter.
     */
    public List<Notification> getByDomainIdAndStatus(String domainId, NotificationStatus status) {
        return notificationRepository.findByDomainIdAndStatus(domainId, status);
    }

    /**
     * Update notification status.
     */
    @Transactional
    public Notification updateStatus(Long id, NotificationStatus status) {
        Notification notification = getById(id);
        notification.setStatus(status);
        return notificationRepository.save(notification);
    }

    /**
     * Add feedback from a user. Only one feedback per user per notification is allowed.
     * If the user has already given feedback, returns the existing state without updating.
     */
    @Transactional
    public NotificationUserState addFeedback(Long notificationId, String userId, String feedbackText) {
        Notification notification = getById(notificationId);

        return notificationUserStateRepository.findByNotificationIdAndUserId(notificationId, userId)
                .map(existing -> {
                    if (existing.getFeedbackText() != null && !existing.getFeedbackText().isBlank()) {
                        return existing; // Already has feedback, do not overwrite
                    }
                    existing.setFeedbackText(feedbackText);
                    existing.setFeedbackCreatedAt(LocalDateTime.now());
                    existing.setAcknowledgedAt(LocalDateTime.now());
                    return notificationUserStateRepository.save(existing);
                })
                .orElseGet(() -> {
                    NotificationUserState state = new NotificationUserState();
                    state.setNotificationId(notificationId);
                    state.setUserId(userId);
                    state.setFeedbackText(feedbackText);
                    state.setFeedbackCreatedAt(LocalDateTime.now());
                    state.setAcknowledgedAt(LocalDateTime.now());
                    state.setRead(true);
                    return notificationUserStateRepository.save(state);
                });
    }

    /**
     * Mark notification as read for a user.
     */
    @Transactional
    public NotificationUserState markAsRead(Long notificationId, String userId) {
        getById(notificationId);

        return notificationUserStateRepository.findByNotificationIdAndUserId(notificationId, userId)
                .map(existing -> {
                    existing.setRead(true);
                    existing.setAcknowledgedAt(LocalDateTime.now());
                    return notificationUserStateRepository.save(existing);
                })
                .orElseGet(() -> {
                    NotificationUserState state = new NotificationUserState();
                    state.setNotificationId(notificationId);
                    state.setUserId(userId);
                    state.setRead(true);
                    state.setAcknowledgedAt(LocalDateTime.now());
                    return notificationUserStateRepository.save(state);
                });
    }

    /**
     * Mark notification as resolved for a user.
     */
    @Transactional
    public NotificationUserState markAsResolved(Long notificationId, String userId) {
        getById(notificationId);

        return notificationUserStateRepository.findByNotificationIdAndUserId(notificationId, userId)
                .map(existing -> {
                    existing.setResolved(true);
                    existing.setAcknowledgedAt(LocalDateTime.now());
                    return notificationUserStateRepository.save(existing);
                })
                .orElseGet(() -> {
                    NotificationUserState state = new NotificationUserState();
                    state.setNotificationId(notificationId);
                    state.setUserId(userId);
                    state.setResolved(true);
                    state.setAcknowledgedAt(LocalDateTime.now());
                    return notificationUserStateRepository.save(state);
                });
    }

    /**
     * Check if user has already given feedback for this notification.
     */
    public boolean hasUserFeedback(Long notificationId, String userId) {
        return notificationUserStateRepository.findByNotificationIdAndUserId(notificationId, userId)
                .map(state -> state.getFeedbackText() != null && !state.getFeedbackText().isBlank())
                .orElse(false);
    }

    /**
     * Get notification with user states as DTO.
     */
    public NotificationResponseDto getNotificationWithUserStates(Long id) {
        Notification notification = getById(id);
        List<NotificationUserStateDto> userStates = notificationUserStateRepository.findByNotificationId(id)
                .stream()
                .map(this::toUserStateDto)
                .collect(Collectors.toList());

        return new NotificationResponseDto(
                notification.getId(),
                notification.getDomainId(),
                notification.getTitle(),
                notification.getMessage(),
                notification.getRecommendedAction(),
                notification.getSeverity(),
                notification.getStatus(),
                notification.getNotificationType(),
                notification.getTicketId(),
                notification.getCreatedAt(),
                userStates
        );
    }

    /**
     * Get the count of all the notifications (used on dashboard).
     */
    public long getNotificationsCount() {
        return notificationRepository.count();
    }

    /**
     * Get the count of all the notifications with severity 'Critical' (used on dashboard).
     */
    public long getNotificationsCriticalCount() {
        return notificationRepository.countBySeverity(NotificationSeverity.CRITICAL);
    }

    private NotificationUserStateDto toUserStateDto(NotificationUserState state) {
        return new NotificationUserStateDto(
                state.getId(),
                state.getUserId(),
                state.getRead(),
                state.getResolved(),
                state.getAcknowledgedAt(),
                state.getFeedbackText(),
                state.getFeedbackCreatedAt()
        );
    }
}
