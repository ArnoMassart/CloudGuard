package com.cloudmen.cloudguard.dto.devices;

import lombok.Getter;

@Getter
public enum DeviceStatus {
    ALL("Alle statussen"),
    APPROVED("Approved"),
    PENDING("Pending"),
    BLOCKED("Blocked");

    private final String value;

    DeviceStatus(String value) {
        this.value = value;
    }

    // Helper om veilig van String naar Enum te gaan
    public static DeviceStatus fromString(String text) {
        for (DeviceStatus b : DeviceStatus.values()) {
            if (b.value.equalsIgnoreCase(text)) {
                return b;
            }
        }
        return ALL; // Default als er niets matcht
    }
}
