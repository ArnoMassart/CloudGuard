package com.cloudmen.cloudguard.dto.oauth;

import com.cloudmen.cloudguard.dto.password.SecurityScoreBreakdownDto;

public record OAuthOverviewResponse(
        long totalThirdPartyApps,
        long totalHighRiskApps,
        long totalPermissionsGranted,
        int securityScore,
        SecurityScoreBreakdownDto securityScoreBreakdown
) {
}
