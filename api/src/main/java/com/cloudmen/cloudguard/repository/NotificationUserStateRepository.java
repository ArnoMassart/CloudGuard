package com.cloudmen.cloudguard.repository;

import com.cloudmen.cloudguard.domain.notifications.NotificationUserState;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface NotificationUserStateRepository extends JpaRepository<NotificationUserState, Long> {

    Optional<NotificationUserState> findByNotificationIdAndUserId(Long notificationId, String userId);

    List<NotificationUserState> findByNotificationId(Long notificationId);

    List<NotificationUserState> findByUserId(String userId);

    boolean existsByNotificationIdAndUserId(Long notificationId, String userId);
}
