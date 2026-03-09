package com.cloudmen.cloudguard.repository;

import com.cloudmen.cloudguard.domain.feedback.NotificationFeedback;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface NotificationFeedbackRepository extends JpaRepository<NotificationFeedback, Long> {
    Optional<NotificationFeedback> findByUserIdAndSourceAndNotificationType(String userId, String source, String notificationType);
}
