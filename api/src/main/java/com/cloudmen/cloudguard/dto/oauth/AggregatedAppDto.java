package com.cloudmen.cloudguard.dto.oauth;

import java.util.List;

/**
 * A Data Transfer Object (DTO) representing an aggregated summary of OAuth or third-party application installed
 * within the organization. <p>
 *
 * This record consolidates application details and risk metrics across the entire user base, providing insights into
 * user exposure, developer verification status, and the specific data access scopes the application has been granted.
 *
 * @param id                    the unique identifier for the application (typically the OAuth client ID)
 * @param name                  the display name of the application
 * @param appType               the category or platform type of the application (e.g., WEB, IOS, ANDROID)
 * @param appSource             the publisher, developer, or origin of the application
 * @param isThirdParty          {@code true} if the application is developed by an external, non-Google entity
 * @param isAnonymous           {@code true} if the developer or publisher is unverified or anonymous
 * @param isHighRisk            {@code true} if the application is deemed a security risk based on its scopes or origin
 * @param totalUsers            the total number of users in the organization who have authorized this application
 * @param exposurePercentage    the percentage of the organization's total user base exposed to this application
 * @param scopeCount            the total number of distinct OAuth scopes requested by the application
 * @param dataAccess            a list detailing the specific data access permissions or APIs the application can
 *                              interact with
 * @param highRiskCount         the total count of high-risk scopes or security flags associated with the application
 */
public record AggregatedAppDto(
        String id,
        String name,
        String appType,
        String appSource,
        boolean isThirdParty,
        boolean isAnonymous,
        boolean isHighRisk,
        int totalUsers,
        int exposurePercentage,
        int scopeCount,
        List<DataAccessDto> dataAccess,
        int highRiskCount
) {
}
