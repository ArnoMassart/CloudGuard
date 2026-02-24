package com.cloudmen.cloudguard.dto.devices;

public record MobileDeviceDetail(
        String resourceId,
        String userName,
        String userEmail,
        String deviceName,
        String model,
        String os,
        String lastSync,
        String status,
        int complianceScore,
        boolean isScreenLockSecure,
        String screenLockText,
        boolean isEncryptionSecure,
        String encryptionText,
        boolean isOsSecure,
        String osText,
        boolean isIntegritySecure,
        String integrityText
) {
}
