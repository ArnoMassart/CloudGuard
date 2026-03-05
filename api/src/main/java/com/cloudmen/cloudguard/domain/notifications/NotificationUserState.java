package com.cloudmen.cloudguard.domain.notifications;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

@Getter
@Setter
@Entity
@Table(
        name = "notification_user_state",
        uniqueConstraints = {
                @UniqueConstraint(columnNames = {"notification_id", "user_id"})
        }
)
public class NotificationUserState {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "notification_id")
    private Long notificationId;

    @Column(name = "user_id")
    private String userId;

    @Column(name = "read")
    private Boolean read = false;

    @Column(name = "resolved")
    private Boolean resolved = false;

    @Column(name = "acknowledged_at")
    private LocalDateTime acknowledgedAt;

    @Column(name = "feedback_text")
    private String feedbackText;

    @Column(name = "feedback_created_at")
    private LocalDateTime feedbackCreatedAt;
}
