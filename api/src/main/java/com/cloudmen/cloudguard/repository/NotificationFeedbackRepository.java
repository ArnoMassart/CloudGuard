package com.cloudmen.cloudguard.repository;

import com.cloudmen.cloudguard.domain.model.feedback.NotificationFeedback;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;
import java.util.Optional;

/**
 * {@link NotificationFeedback} persistence ({@code notification_feedback}).
 */
public interface NotificationFeedbackRepository extends JpaRepository<NotificationFeedback, Long> {

    /** Rows with non-empty trimmed feedback text (used for global “already reported” hints). */
    @Query("SELECT f FROM NotificationFeedback f WHERE f.feedbackText IS NOT NULL AND LENGTH(TRIM(f.feedbackText)) > 0")
    List<NotificationFeedback> findAllWithFeedback();

    Optional<NotificationFeedback> findByUserIdAndSourceAndNotificationType(String userId, String source, String notificationType);

    List<NotificationFeedback> findByUserId(String userId);
}
