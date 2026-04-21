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

    List<NotificationInstance> findByOrganizationIdAndStatus(
            Long organizationId, NotificationInstanceStatus status);

    List<NotificationInstance> findByOrganizationIdAndStatusAndSeverity(
            Long organizationId,
            NotificationInstanceStatus status,
            NotificationSeverity severity);

    Optional<NotificationInstance> findByOrganizationIdAndSourceAndNotificationType(
            Long organizationId, String source, String notificationType);

    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query(
            "DELETE FROM NotificationInstance n WHERE n.status = :status AND n.solvedAt IS NOT NULL AND n.solvedAt < :cutoff")
    int deleteByStatusAndSolvedAtBefore(
            @Param("status") NotificationInstanceStatus status, @Param("cutoff") LocalDateTime cutoff);
}
