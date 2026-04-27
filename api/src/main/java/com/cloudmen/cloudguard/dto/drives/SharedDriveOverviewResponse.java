package com.cloudmen.cloudguard.dto.drives;

import com.cloudmen.cloudguard.dto.password.SecurityScoreBreakdownDto;
import com.cloudmen.cloudguard.dto.preferences.SectionWarningsDto;

/**
 * A Data Transfer Object (DTO) providing an aggregated overview of the organization's Shared Drives. <p>
 *
 * This record encapsulates high-level metrics such as total drive counts, orphaned drives, external membership
 * statistics, and an overall security score along with its detailed breakdown.
 *
 * @param totalDrives                       the total number of shared drives within the organization
 * @param orphanDrives                      the number of shared drives lacking an active organizer or manager
 * @param totalHighRisk                     the number of shared drives classified as having a high security risk
 * @param totalExternalMembersCount         the total sum of external users across all shared drives
 * @param securityScore                     the aggregated security score evaluating the health of the shared drives
 * @param notOnlyDomainUsersAllowedCount    the number of drives that permit access to users outside the domain
 * @param notOnlyMembersCanAccessCount      the number of drives where non-members can access files via shared links
 * @param externalMembersDriveCount         the number of distinct drives that contain at least one external member
 * @param securityScoreBreakdown            the detailed breakdown of how the security score was calculated
 * @param warnings                          any section-specific warnings or critical alerts related to shared drives
 */
public record SharedDriveOverviewResponse(
        int totalDrives,
        int orphanDrives,
        int totalHighRisk,
        int totalExternalMembersCount,
        int securityScore,
        int notOnlyDomainUsersAllowedCount,
        int notOnlyMembersCanAccessCount,
        int externalMembersDriveCount,
        SecurityScoreBreakdownDto securityScoreBreakdown,
        SectionWarningsDto warnings
) {}
