package com.cloudmen.cloudguard.dto.devices;

import lombok.Getter;

/**
 * An enumeration representing the various operational or compliance statuses a device can hold within the system. <p>
 *
 * This enum is used to filter or categorized devices based on their current approval state, providing a safe fallback
 * mechanism for unknown or generalized queries.
 */
@Getter
public enum DeviceStatus {
    ALL("Alle statussen"),
    APPROVED("Approved"),
    PENDING("Pending"),
    BLOCKED("Blocked");

    private final String value;

    /**
     * Constructs a new {@link DeviceStatus} with its corresponding string representation.
     *
     * @param value the descriptive string value representing the status
     */
    DeviceStatus(String value) {
        this.value = value;
    }

    /**
     * Safely converts a string value into its corresponding {@link DeviceStatus} enum constant.
     *
     * @param text the string representation of the device status to match
     * @return the matching {@link DeviceStatus}, or {@link #ALL} if no exact match is found
     */
    public static DeviceStatus fromString(String text) {
        for (DeviceStatus b : DeviceStatus.values()) {
            if (b.value.equalsIgnoreCase(text)) {
                return b;
            }
        }
        return ALL;
    }
}
