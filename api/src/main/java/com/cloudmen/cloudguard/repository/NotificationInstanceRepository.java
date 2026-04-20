package com.cloudmen.cloudguard.repository;

import com.cloudmen.cloudguard.domain.model.notification.NotificationInstance;
import com.cloudmen.cloudguard.domain.model.notification.NotificationInstanceStatus;
import com.cloudmen.cloudguard.domain.model.notification.NotificationSeverity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface NotificationInstanceRepository extends JpaRepository<NotificationInstance, Long> {
    List<NotificationInstance> findByOrganizationId(Long organizationId);

    boolean existsByOrganizationId(Long organizationId);

    List<NotificationInstance> findByOrganizationIdAndStatus(
            Long organizationId, NotificationInstanceStatus status);

    List<NotificationInstance> findByOrganizationIdAndStatusAndSeverity(
            Long organizationId,
            NotificationInstanceStatus status,
            NotificationSeverity severity);

    Optional<NotificationInstance> findByOrganizationIdAndSourceAndNotificationType(
            Long organizationId, String source, String notificationType);
}
