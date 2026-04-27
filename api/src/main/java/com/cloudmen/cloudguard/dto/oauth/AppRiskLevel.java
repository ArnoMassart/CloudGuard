package com.cloudmen.cloudguard.dto.oauth;

import lombok.Getter;

/**
 * Enum representing the varying levels of security risk associated with third-party or OAuth applications. <p>
 *
 * This enum categorizes applications into distinct risk tiers based on factors like their data access scopes,
 * developer verification, and origins, providing a human-readable label.
 */
@Getter
public enum AppRiskLevel {
    /**
     * Represents an application that poses a significant security risk to the organization, often due to overly broad
     * data access scopes or an unverified developer.
     */
    HIGH("Hoog risico"),

    /**
     * Represents an application that poses a moderate security risk, requiring standard administrative monitoring
     * and oversight.
     */
    MEDIUM("Gemiddeld risico"),

    /**
     * Represents an application that is considered safe or poses a minimal security risk, typically requesting basic
     * sign-in or non-sensitive read permissions.
     */
    LOW("Laag risico");

    private final String label;

    /**
     * Constructs a new {@link AppRiskLevel} with its corresponding human-readable label.
     *
     * @param label the descriptive string value representing the risk level (e.g., "Hoog risico")
     */
    AppRiskLevel(String label) {
        this.label = label;
    }
}
