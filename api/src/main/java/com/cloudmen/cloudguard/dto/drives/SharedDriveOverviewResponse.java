package com.cloudmen.cloudguard.dto.drives;

import com.cloudmen.cloudguard.dto.password.SecurityScoreBreakdownDto;

public record SharedDriveOverviewResponse(
        int totalDrives,
        int orphanDrives,
        int totalHighRisk,
        int totalExternalMembersCount,
        int securityScore,
        int notOnlyDomainUsersAllowedCount,
        int notOnlyMembersCanAccessCount,
        int externalMembersDriveCount,
        SecurityScoreBreakdownDto securityScoreBreakdown
)
{ }
