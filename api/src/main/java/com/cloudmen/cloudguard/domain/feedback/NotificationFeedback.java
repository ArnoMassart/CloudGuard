package com.cloudmen.cloudguard.domain.feedback;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDate;
import java.time.LocalDateTime;

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
