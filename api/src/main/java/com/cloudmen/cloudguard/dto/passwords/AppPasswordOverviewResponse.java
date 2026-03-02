package com.cloudmen.cloudguard.dto.passwords;

public record AppPasswordOverviewResponse(
        boolean allowed,
        int totalAppPasswords,
        int totalHighRiskAppPasswords,
        int securityScore
) {
}
