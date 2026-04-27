package com.cloudmen.cloudguard.dto.oauth;

import java.util.List;

/**
 * A Data Transfer Object (DTO) representing a raw, unaggregated OAuth token granted by a specific user. <p>
 *
 * This record holds the baseline data retrieved directly from the Google Workspace API for a single application
 * authorization. It includes user details, application identifiers, requested scopes, and flags indicating the
 * application's nature and verification status.
 *
 * @param userEmail     the email address of the user who authorized the application
 * @param clientId      the unique OAuth client identifier for the authorized application
 * @param displayText   the human-readable name or display text of the application
 * @param scopes        a list of specific OAuth scopes or data access permissions granted by the user
 * @param isNativeApp   {@code true} if the application is installed natively on a device (e.g., iOS, Android or
 *                      desktop) rather than running as a web application
 * @param isAnonymous   {@code true} if the developer or publisher of the application is unverified or anonymous
 */
public record RawUserToken(
        String userEmail,
        String clientId,
        String displayText,
        List<String> scopes,
        boolean isNativeApp,
        boolean isAnonymous
) {
}
