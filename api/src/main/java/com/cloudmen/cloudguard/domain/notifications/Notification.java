package com.cloudmen.cloudguard.domain.notifications;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

@Getter
@Setter
@Entity(name="tbl_notifications")
public class Notification {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String domainId;
    private String title;
    private String message;
    private String recommendedAction;

    @Enumerated(EnumType.STRING)
    private NotificationSeverity severity;

    @Enumerated(EnumType.STRING)
    private NotificationStatus status;

    private String notificationType; //avoid duplication of notification. if domain + notificationType is the same, don't create a new notif
    private String ticketId;
    private LocalDateTime createdAt;
}
