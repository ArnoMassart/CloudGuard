package com.cloudmen.cloudguard.dto.apppasswords;

public record AppPasswordOverviewResponse(
        boolean allowed,
        int totalAppPasswords,
        int totalHighRiskAppPasswords,
        int securityScore
) {
}
