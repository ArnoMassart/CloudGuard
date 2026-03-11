package com.cloudmen.cloudguard.repository;

import com.cloudmen.cloudguard.domain.feedback.ResolvedNotification;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public interface ResolvedNotificationRepository extends JpaRepository<ResolvedNotification, Long> {

    List<ResolvedNotification> findByUserIdOrderByResolvedAtDesc(String userId);

    Optional<ResolvedNotification> findByUserIdAndSourceAndNotificationType(String userId, String source, String notificationType);

    boolean existsByUserIdAndSourceAndNotificationType(String userId, String source, String notificationType);

    @Modifying
    @Query("DELETE FROM ResolvedNotification r WHERE r.resolvedAt < :before")
    int deleteByResolvedAtBefore(LocalDateTime before);
}
