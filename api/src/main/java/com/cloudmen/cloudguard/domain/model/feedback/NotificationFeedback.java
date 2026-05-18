package com.cloudmen.cloudguard.domain.model.feedback;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

/**
 * One feedback submission per user and notification identity ({@code user_id}, {@code source}, {@code notification_type}).
 * Text is emailed to operators when configured; blank rows may exist as placeholders until first submit.
 */
@Entity
@Table(name="notification_feedback", uniqueConstraints = {
        @UniqueConstraint(columnNames = {"user_id","source","notification_type"})
})
@Getter
@Setter
public class NotificationFeedback {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String userId;
    private String source;
    private String notificationType;
    private String domainId;
    private String feedbackText;
    private LocalDateTime createdAt;
}
