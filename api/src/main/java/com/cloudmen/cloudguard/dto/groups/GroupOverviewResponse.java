package com.cloudmen.cloudguard.dto.groups;

import com.cloudmen.cloudguard.dto.password.SecurityScoreBreakdownDto;
import com.cloudmen.cloudguard.dto.preferences.SectionWarningsDto;

public record GroupOverviewResponse(
        int totalGroups,
        int groupsWithExternal,
        int highRiskGroups,
        int mediumRiskGroups,
        int lowRiskGroups,
        int securityScore,
        SecurityScoreBreakdownDto securityScoreBreakdown,
        SectionWarningsDto warnings
) {}
