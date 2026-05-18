package com.cloudmen.cloudguard.dto.password;

import java.util.List;

/**
 * UI-ready decomposition of {@link PasswordSettingsOverviewResponse#securityScore()}.
 *
 * @param totalScore headline rounded score identical to overview when present
 * @param status     coarse band label from {@link com.cloudmen.cloudguard.utility.GoogleServiceHelperMethods#getOverviewStatus(int)}
 * @param factors    ordered factor rows with titles, descriptions, scores, and severity hints
 */
public record SecurityScoreBreakdownDto(
        int totalScore,
        String status,
        List<SecurityScoreFactorDto> factors
) {
}
