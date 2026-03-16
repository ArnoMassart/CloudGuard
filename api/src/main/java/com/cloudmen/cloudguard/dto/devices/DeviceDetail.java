package com.cloudmen.cloudguard.dto.devices;

public record DeviceDetail(
        String resourceId,
        String deviceType,
        String userName,
        String userEmail,
        String deviceName,
        String model,
        String os,
        String lastSync,
        String status,
        int complianceScore,
        boolean lockSecure,
        String screenLockText,
        boolean encSecure,
        String encryptionText,
        boolean osSecure,
        String osText,
        boolean intSecure,
        String integrityText
) {
}
