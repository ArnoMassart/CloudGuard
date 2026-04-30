package com.cloudmen.cloudguard.dto.oauth;

import com.cloudmen.cloudguard.dto.password.SecurityScoreBreakdownDto;

/**
 * A Data Transfer Object (DTO) providing a high-level summary of the organization's OAuth and third-party
 * application landscape. <p>
 *
 * This record encapsulates key metrics such as total installed apps, high-risk applications, granted permissions,
 * and an overall security score.
 *
 * @param totalThirdPartyApps       the total number of distinct third-party applications authorized by users across
 *                                  the organization
 * @param totalHighRiskApps         the total number of authorized applications flagged as posing a high security risk
 * @param totalPermissionsGranted   the aggregate count of all OAuth scopes or data access permissions granted across
 *                                  all applications
 * @param securityScore             a calculated numerical value representing the overall security posture concerning
 *                                  third-party application access
 * @param securityScoreBreakdown    a detailed breakdown explaining the specific factors and metrics that contributed
 *                                  to the overall security score
 */
public record OAuthOverviewResponse(
        long totalThirdPartyApps,
        long totalHighRiskApps,
        long totalPermissionsGranted,
        int securityScore,
        SecurityScoreBreakdownDto securityScoreBreakdown
) {
}
