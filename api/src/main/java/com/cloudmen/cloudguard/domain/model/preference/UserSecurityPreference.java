package com.cloudmen.cloudguard.domain.model.preference;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * One row of organization-wide security preferences ({@code user_security_preference}).
 * Scoped by {@code organization_id}; optional legacy {@code user_id} column may be unused for current writes.
 * Boolean toggles mute notifications / scoring dimensions; {@code preference_value} stores DNS importance enums for {@code imp*} keys.
 */
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

    /** Tenant owning this preference row. */
    @Column(name = "organization_id", nullable = false)
    private Long organizationId;

    /** Optional legacy linkage; primary scoping is organizational. */
    @Column(name = "user_id")
    private String userId;

    /** Settings area (e.g. {@code users-groups}, {@code domain-dns}). */
    private String section;

    /** Toggle key or DNS importance key ({@code impSpf}, …). */
    private String preferenceKey;

    /** When {@code false}, matching checks/notifications are suppressed for the org. */
    private boolean enabled = true;

    /** Optional string value (e.g. DNS importance: REQUIRED, RECOMMENDED, OPTIONAL). */
    @Column(name = "preference_value", length = 64)
    private String preferenceValue;

    @Column(name = "updated_at")
    private java.time.LocalDateTime updatedAt;
}
