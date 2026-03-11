package com.cloudmen.cloudguard.dto.oauth;

public record OAuthOverviewResponse(
        long totalThirdPartyApps,
        long totalHighRiskApps,
        long totalPermissionsGranted,
        int securityScore
) {
}
