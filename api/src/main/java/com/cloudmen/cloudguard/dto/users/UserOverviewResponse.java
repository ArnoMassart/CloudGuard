package com.cloudmen.cloudguard.dto.users;

import com.cloudmen.cloudguard.dto.password.SecurityScoreBreakdownDto;

public record UserOverviewResponse(
            int totalUsers,
            int withoutTwoFactor,
            int adminUsers,
            int securityScore,
            int activeLongNoLoginCount,
            int inactiveRecentLoginCount,
            SecurityScoreBreakdownDto securityScoreBreakdown
){}
