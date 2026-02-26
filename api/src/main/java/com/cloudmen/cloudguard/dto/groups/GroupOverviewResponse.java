package com.cloudmen.cloudguard.dto.groups;

public record GroupOverviewResponse(
        long totalGroups,
        long groupsWithExternal,
        long highRiskGroups,
        long mediumRiskGroups,
        long lowRiskGroups,
        int securityScore
) {}
