package com.cloudmen.cloudguard.dto.o_Auth;

public record OAuthOverviewResponse(
        long totalThirdPartyApps,
        long totalHighRiskApps,
        long totalPermissionsGranted,
        int securityScore
) {
}
