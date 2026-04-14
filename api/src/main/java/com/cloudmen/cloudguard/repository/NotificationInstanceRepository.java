package com.cloudmen.cloudguard.repository;

import com.cloudmen.cloudguard.domain.model.notification.NotificationInstance;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface NotificationInstanceRepository extends JpaRepository<NotificationInstance, String> {
    List<NotificationInstance> findByOrganizationId(Long organizationId);

    List<NotificationInstance> findByOrganizationIdAndStatus(Long organizationId, String status);
    
    List<NotificationInstance> findByOrganizationIdAndSourceAndNotificationType(Long organizationId, String source, String notificationType);
}
