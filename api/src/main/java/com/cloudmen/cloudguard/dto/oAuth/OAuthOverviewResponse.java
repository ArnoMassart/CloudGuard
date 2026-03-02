package com.cloudmen.cloudguard.dto.oAuth;

public record OAuthOverviewResponse(
        long totalThirdPartyApps,
        long totalHighRiskApps,
        long totalPermissionsGranted,
        int securityScore,
        long totalApps
) {
}
