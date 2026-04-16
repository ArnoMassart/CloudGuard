package com.cloudmen.cloudguard.domain.model.notification;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(
        name="tbl_notifications",
        uniqueConstraints =
                @UniqueConstraint(columnNames = {"organization_id","source","notification_type"})
)
@Getter
@Setter
@NoArgsConstructor
public class NotificationInstance {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name="organization_id", nullable=false)
    private Long organizationId;

    @Column(nullable=false, length=64)
    private String source;

    @Column(name="notification_type", nullable=false, length=128)
    private String notificationType;

    @Enumerated(EnumType.STRING)
    @Column(nullable=false, length=16)
    private NotificationInstanceStatus status = NotificationInstanceStatus.ACTIVE;

    @Enumerated(EnumType.STRING)
    @Column(nullable=false, length=32)
    private NotificationSeverity severity;

    @Column(length=512)
    private String title;

    @Lob
    private String description;

    @ElementCollection(fetch=FetchType.EAGER)
    @CollectionTable(
            name="notification_instance_actions",
            joinColumns=@JoinColumn(name="notification_instance_id"))
    @Column(name="action")
    private List<String> recommendedActions = new ArrayList<>();

    @Column(name="created_at")
    private LocalDateTime createdAt;

    @Column(name="updated_at")
    private LocalDateTime updatedAt;

    @Column(name = "source_label", length = 256)
    private String sourceLabel;

    @Column(name = "source_route", length = 256)
    private String sourceRoute;

    @Column(name = "first_observed_at")
    private LocalDateTime firstObservedAt;

    @Column(name = "last_observed_at")
    private LocalDateTime lastObservedAt;

    @Column(name = "solved_at")
    private LocalDateTime solvedAt;

    @PrePersist
    void onCreate() {
        LocalDateTime now = LocalDateTime.now();
        createdAt = now;
        updatedAt = now;
    }

    @PreUpdate
    void onUpdate() {
        updatedAt = LocalDateTime.now();
    }

}
