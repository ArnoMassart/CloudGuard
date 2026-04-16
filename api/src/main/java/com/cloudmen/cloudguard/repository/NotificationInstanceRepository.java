package com.cloudmen.cloudguard.repository;

import com.cloudmen.cloudguard.domain.model.notification.NotificationInstance;
import com.cloudmen.cloudguard.domain.model.notification.NotificationInstanceStatus;
import com.cloudmen.cloudguard.domain.model.notification.NotificationSeverity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public interface NotificationInstanceRepository extends JpaRepository<NotificationInstance, Long> {
    List<NotificationInstance> findByOrganizationId(Long organizationId);

    boolean existsByOrganizationId(Long organizationId);

    boolean existsByOrganizationIdAndDismissedAtIsNull(Long organizationId);

    List<NotificationInstance> findByOrganizationIdAndStatus(
            Long organizationId, NotificationInstanceStatus status);

    List<NotificationInstance> findByOrganizationIdAndStatusAndDismissedAtIsNull(
            Long organizationId, NotificationInstanceStatus status);

    List<NotificationInstance> findByOrganizationIdAndDismissedAtIsNotNullOrderByDismissedAtDesc(
            Long organizationId);

    List<NotificationInstance> findByOrganizationIdAndStatusAndSeverityAndDismissedAtIsNull(
            Long organizationId,
            NotificationInstanceStatus status,
            NotificationSeverity severity);

    Optional<NotificationInstance> findByOrganizationIdAndSourceAndNotificationType(
            Long organizationId, String source, String notificationType);

    @Modifying
    @Query(
            "UPDATE NotificationInstance n SET n.dismissedAt = NULL WHERE n.dismissedAt IS NOT NULL AND n.dismissedAt < :before")
    int clearDismissedAtBefore(@Param("before") LocalDateTime before);
}
