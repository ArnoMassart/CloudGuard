package com.cloudmen.cloudguard.domain.feedback;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;
import java.util.List;

@Entity
@Table(
        name = "resolved_notification",
        uniqueConstraints = {
                @UniqueConstraint(columnNames = {"user_id", "source", "notification_type"})
        }
)
@Getter
@Setter
public class ResolvedNotification {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "user_id")
    private String userId;

    private String source;
    private String notificationType;
    private String sourceLabel;
    private String sourceRoute;
    private String title;
    private String description;
    private String severity;

    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(name = "resolved_notification_actions", joinColumns = @JoinColumn(name = "resolved_notification_id"))
    @Column(name = "action")
    private List<String> recommendedActions;

    @Column(name = "resolved_at")
    private LocalDateTime resolvedAt;
}
