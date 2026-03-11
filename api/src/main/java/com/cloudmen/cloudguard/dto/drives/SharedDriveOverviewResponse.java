package com.cloudmen.cloudguard.dto.drives;

public record SharedDriveOverviewResponse(
        int totalDrives,
        int orphanDrives,
        int totalHighRisk,
        int totalExternalMembersCount,
        int securityScore,
        int notOnlyDomainUsersAllowedCount,
        int notOnlyMembersCanAccessCount,
        int externalMembersDriveCount
)
{ }
