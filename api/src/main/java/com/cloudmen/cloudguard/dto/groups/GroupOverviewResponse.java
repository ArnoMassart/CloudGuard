package com.cloudmen.cloudguard.dto.groups;

import com.cloudmen.cloudguard.dto.password.SecurityScoreBreakdownDto;

public record GroupOverviewResponse(
        long totalGroups,
        long groupsWithExternal,
        long highRiskGroups,
        long mediumRiskGroups,
        long lowRiskGroups,
        int securityScore,
        SecurityScoreBreakdownDto securityScoreBreakdown
) {}
