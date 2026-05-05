package com.cloudmen.cloudguard.dto.apppasswords;

import com.cloudmen.cloudguard.dto.password.SecurityScoreBreakdownDto;

public record AppPasswordOverviewResponse(
        boolean allowed,
        int totalAppPasswords,
        int usersWithAppPasswords,
        Integer securityScore,
        SecurityScoreBreakdownDto securityScoreBreakdown
) {
}
