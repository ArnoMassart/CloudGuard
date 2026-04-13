package com.cloudmen.cloudguard.domain.model.preference;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(
        name = "user_security_preference",
        uniqueConstraints = @UniqueConstraint(columnNames = {"organization_id", "section", "preference_key"})
)
@Getter
@Setter
@NoArgsConstructor
public class UserSecurityPreference {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "organization_id", nullable = false)
    private Long organizationId;

    @Column(name = "user_id")
    private String userId;

    private String section;
    private String preferenceKey;

    private boolean enabled = true;

    /** Optional string value (e.g. DNS importance: REQUIRED, RECOMMENDED, OPTIONAL). */
    @Column(name = "preference_value", length = 64)
    private String preferenceValue;

    @Column(name = "updated_at")
    private java.time.LocalDateTime updatedAt;
}
