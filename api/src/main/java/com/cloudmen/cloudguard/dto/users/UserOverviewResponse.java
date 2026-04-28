package com.cloudmen.cloudguard.dto.users;

import com.cloudmen.cloudguard.dto.password.SecurityScoreBreakdownDto;
import com.cloudmen.cloudguard.dto.preferences.SectionWarningsDto;

/**
 * A Data Transfer Object (DTO) providing a high-level overview and security summary of the organization's users. <p>
 *
 * This record aggregates key metrics such as total user counts, Two-Factor Authentication (2FA) adoption rates,
 * administrative privileges, and specific security anomalies (like active users with stale logins). It also includes
 * an overall security score and relevant compliance warnings.
 *
 * @param totalUsers                the total number of users present within the organization
 * @param withoutTwoFactor          the number of uses who do not have Two-Factor Authentication (2FA) enabled
 * @param adminUsers                the total number of users who hold administrative privileges
 * @param securityScore             a calculated numerical value representing the overall security posture of user
 *                                  accounts
 * @param activeLongNoLoginCount    the number of users marked as active who have not logged in for an extended period
 * @param inactiveRecentLoginCount  the number of users flagged as inactive who have logged in recently, indicating
 *                                  a potential status mismatch
 * @param securityScoreBreakdown    a detailed breakdown explaining the specific metrics that contribute to the security
 *                                  score
 * @param warnings                  a collection of section-specific warnings generated based on the organization's
 *                                  security preferences
 */
public record UserOverviewResponse(
            int totalUsers,
            int withoutTwoFactor,
            int adminUsers,
            int securityScore,
            int activeLongNoLoginCount,
            int inactiveRecentLoginCount,
            SecurityScoreBreakdownDto securityScoreBreakdown,
            SectionWarningsDto warnings
){}
