package com.cloudmen.cloudguard.dto.devices;

import com.cloudmen.cloudguard.dto.password.SecurityScoreBreakdownDto;
import com.cloudmen.cloudguard.dto.preferences.SectionWarningsDto;

public record DeviceOverviewResponse(
        int totalDevices,
        int totalNonCompliant,
        int totalApprovedDevices,
        int securityScore,
        int lockScreenCount,
        int encryptionCount,
        int osVersionCount,
        int integrityCount,
        SecurityScoreBreakdownDto securityScoreBreakdown,
        SectionWarningsDto warnings
) {}
