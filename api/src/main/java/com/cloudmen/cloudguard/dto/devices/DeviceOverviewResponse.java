package com.cloudmen.cloudguard.dto.devices;

public record DeviceOverviewResponse(
        int totalDevices,
        int totalNonCompliant,
        int totalApprovedDevices,
        int securityScore,
        int lockScreenCount,
        int encryptionCount,
        int osVersionCount,
        int integrityCount
) {
}
