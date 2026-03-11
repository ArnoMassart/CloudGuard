package com.cloudmen.cloudguard.dto.devices;

import lombok.Getter;

@Getter
public enum MobileDeviceStatus {
    ALL("Alle statussen"),
    APPROVED("Approved"),
    PENDING("Pending"),
    BLOCKED("Blocked");

    private final String value;

    MobileDeviceStatus(String value) {
        this.value = value;
    }

    // Helper om veilig van String naar Enum te gaan
    public static MobileDeviceStatus fromString(String text) {
        for (MobileDeviceStatus b : MobileDeviceStatus.values()) {
            if (b.value.equalsIgnoreCase(text)) {
                return b;
            }
        }
        return ALL; // Default als er niets matcht
    }
}
