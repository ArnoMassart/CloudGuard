package com.cloudmen.cloudguard.dto.users;

import com.cloudmen.cloudguard.dto.password.SecurityScoreBreakdownDto;
import com.cloudmen.cloudguard.dto.preferences.SectionWarningsDto;

public record UserOverviewResponse(
            int totalUsers,
            int withoutTwoFactor,
            int adminUsers,
            int securityScore,
            int activeLongNoLoginCount,
            int inactiveRecentLoginCount,
            SecurityScoreBreakdownDto securityScoreBreakdown,
            SectionWarningsDto warnings
){}
