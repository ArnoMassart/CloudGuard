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

    /**
     * Race-safe insert of the parent {@code tbl_notifications} row for a unique
     * {@code (organization_id, source, notification_type)} key. Uses MySQL's
     * {@code ON DUPLICATE KEY UPDATE id = id} no-op so concurrent writers cannot fail
     * with a duplicate-key violation. Recommended actions ({@code @ElementCollection}
     * child rows) are not inserted here; callers should set them via JPA after reload.
     */
    @Modifying
    @Query(value = """
            INSERT INTO tbl_notifications
                (organization_id, source, notification_type, status, severity,
                 title, description, source_label, source_route, created_at, updated_at)
            VALUES
                (:orgId, :source, :type, :status, :severity,
                 :title, :description, :sourceLabel, :sourceRoute, :now, :now)
            ON DUPLICATE KEY UPDATE id = id
            """, nativeQuery = true)
    int insertIfMissing(
            @Param("orgId") Long orgId,
            @Param("source") String source,
            @Param("type") String type,
            @Param("status") String status,
            @Param("severity") String severity,
            @Param("title") String title,
            @Param("description") String description,
            @Param("sourceLabel") String sourceLabel,
            @Param("sourceRoute") String sourceRoute,
            @Param("now") LocalDateTime now);
}
