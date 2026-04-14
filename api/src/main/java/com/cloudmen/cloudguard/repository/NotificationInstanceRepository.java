package com.cloudmen.cloudguard.repository;

import com.cloudmen.cloudguard.domain.model.notification.NotificationInstance;
import com.cloudmen.cloudguard.domain.model.notification.NotificationInstanceStatus;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface NotificationInstanceRepository extends JpaRepository<NotificationInstance, Long> {
    List<NotificationInstance> findByOrganizationId(Long organizationId);

    List<NotificationInstance> findByOrganizationIdAndStatus(
            Long organizationId, NotificationInstanceStatus status);

    Optional<NotificationInstance> findByOrganizationIdAndSourceAndNotificationType(
            Long organizationId, String source, String notificationType);
}
