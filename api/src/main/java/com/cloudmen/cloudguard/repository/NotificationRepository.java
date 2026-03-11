package com.cloudmen.cloudguard.repository;

import com.cloudmen.cloudguard.domain.notifications.Notification;
import com.cloudmen.cloudguard.domain.notifications.NotificationSeverity;
import com.cloudmen.cloudguard.domain.notifications.NotificationStatus;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface NotificationRepository extends JpaRepository<Notification, Long> {

    Optional<Notification> findByDomainIdAndNotificationType(String domainId, String notificationType);

    List<Notification> findByDomainId(String domainId);

    List<Notification> findByStatus(NotificationStatus status);

    List<Notification> findByDomainIdAndStatus(String domainId, NotificationStatus status);

    long countBySeverity(NotificationSeverity severity);
}
