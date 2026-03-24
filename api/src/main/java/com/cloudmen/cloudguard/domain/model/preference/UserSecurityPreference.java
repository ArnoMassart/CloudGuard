package com.cloudmen.cloudguard.domain.model.preference;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(
        name = "user_security_preference",
        uniqueConstraints = @UniqueConstraint(columnNames = {"user_id", "section", "preference_key"})
)
@Getter
@Setter
@NoArgsConstructor
public class UserSecurityPreference {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "user_id")
    private String userId;

    private String section;
    private String preferenceKey;

    private boolean enabled = true;

    @Column(name = "updated_at")
    private java.time.LocalDateTime updatedAt;
}
