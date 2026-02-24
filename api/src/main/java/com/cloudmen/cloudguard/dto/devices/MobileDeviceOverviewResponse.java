package com.cloudmen.cloudguard.dto.devices;

public record MobileDeviceOverviewResponse(
        long totalDevices,
        long totalNonCompliant,
        long totalApprovedDevices,
        long securityScore,
        long lockScreenCount,
        long encryptionCount,
        long osVersionCount,
        long integrityCount
) {
}
