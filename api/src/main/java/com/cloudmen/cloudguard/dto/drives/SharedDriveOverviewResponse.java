package com.cloudmen.cloudguard.dto.drives;

public record SharedDriveOverviewResponse(
        long totalDrives,
        long orphanDrives,
        long totalHighRisk,
        long totalExternalMembersCount,
        long securityScore,
        long externallySharedCount,
        long noMemberRestrictionsCount,
        long orphanDrivesCount
)
{ }
