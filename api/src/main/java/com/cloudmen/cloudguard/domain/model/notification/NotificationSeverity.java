package com.cloudmen.cloudguard.domain.model.notification;

import java.util.Locale;

/**
 * Persisted severity for {@link NotificationInstance}. DTOs use lowercase strings
 * {@code critical}, {@code warning}, {@code info} from aggregation; legacy values
 * {@code HIGH}/{@code MEDIUM}/{@code LOW} map to those for API responses.
 */
public enum NotificationSeverity {
    LOW,
    MEDIUM,
    HIGH,
    CRITICAL,
    WARNING,
    INFO;

    public static NotificationSeverity fromDtoString(String s) {
        if (s == null || s.isBlank()) {
            return MEDIUM;
        }
        return switch (s.toLowerCase(Locale.ROOT)) {
            case "critical" -> CRITICAL;
            case "warning" -> WARNING;
            case "info" -> INFO;
            case "high" -> HIGH;
            case "low" -> LOW;
            default -> MEDIUM;
        };
    }

    public static String toDtoString(NotificationSeverity s) {
        if (s == null) {
            return "info";
        }
        return switch (s) {
            case CRITICAL -> "critical";
            case WARNING -> "warning";
            case INFO -> "info";
            case HIGH -> "critical";
            case MEDIUM -> "warning";
            case LOW -> "info";
        };
    }
}
