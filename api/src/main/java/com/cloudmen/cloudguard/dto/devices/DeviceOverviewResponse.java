package com.cloudmen.cloudguard.dto.devices;

import com.cloudmen.cloudguard.dto.password.SecurityScoreBreakdownDto;
import com.cloudmen.cloudguard.dto.preferences.SectionWarningsDto;

/**
 * A Data Transfer Object (DTO) providing an aggregated overview of the organization's device fleet. <p>
 *
 * This record encapsulates high-level metrics such as total counts, compliance status, specific security vulnerability
 * counts, and an overall security score along with its detailed breakdown.
 *
 * @param totalDevices              the total number of devices registered in the system
 * @param totalNonCompliant         the number of devices failing to meet security compliance standards
 * @param totalApprovedDevices      the number of devices explicitly approved or fully compliant
 * @param securityScore             the aggregated security score evaluating the overall health of the device fleet
 * @param lockScreenCount           the number of devices flagged for lock screen security issues
 * @param encryptionCount           the number of devices flagged for encryption vulnerabilities
 * @param osVersionCount            the number of devices flagged for outdated or insecure operating systems
 * @param integrityCount            the number of devices flagged for compromised system integrity (e.g., rooted or
 *                                  jailbroken)
 * @param securityScoreBreakdown    the detailed breakdown of how the security score was calculated
 * @param warnings                  any section-specific warnings or critical alerts related to device management
 */
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
