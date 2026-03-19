package com.cloudmen.cloudguard.dto.apppasswords;

import com.cloudmen.cloudguard.dto.password.SecurityScoreBreakdownDto;

public record AppPasswordOverviewResponse(
        boolean allowed,
        int totalAppPasswords,
        int totalHighRiskAppPasswords,
        int usersWithAppPasswords,
        int securityScore,
        SecurityScoreBreakdownDto securityScoreBreakdown
) {
}
