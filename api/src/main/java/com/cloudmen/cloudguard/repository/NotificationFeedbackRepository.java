package com.cloudmen.cloudguard.repository;

import com.cloudmen.cloudguard.domain.model.feedback.NotificationFeedback;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;
import java.util.Optional;

public interface NotificationFeedbackRepository extends JpaRepository<NotificationFeedback, Long> {

    @Query("SELECT f FROM NotificationFeedback f WHERE f.feedbackText IS NOT NULL AND LENGTH(TRIM(f.feedbackText)) > 0")
    List<NotificationFeedback> findAllWithFeedback();
    Optional<NotificationFeedback> findByUserIdAndSourceAndNotificationType(String userId, String source, String notificationType);

    List<NotificationFeedback> findByUserId(String userId);
}
