package com.cloudmen.cloudguard.dto.password;

import com.cloudmen.cloudguard.dto.adminsecuritykeys.AdminWithSecurityKeyDto;

import java.util.List;

/**
 * Aggregated payload for the password-settings screen: OU-level policies, 2SV, forced resets, admin key gaps,
 * optional Reports error text, and the headline score plus breakdown.
 *
 * @param passwordPoliciesByOu             merged Chrome/Workspace password rules per organizational unit path
 * @param twoStepVerification               enrollment/enforcement rollups by OU and tenant totals
 * @param usersWithForcedChange             Directory users with {@code changePasswordAtNextLogin}
 * @param summary                           compact counts backing KPI tiles and scoring inputs
 * @param adminsWithoutSecurityKeys        admins missing security keys (empty when unknown vs error — check {@code adminsSecurityKeysErrorMessage})
 * @param adminsSecurityKeysErrorMessage    non-null when admin-keys lookup failed upstream ({@code null} on success)
 * @param securityScore                     weighted 0–100 tenant score; {@code null} when {@code summary.totalUsers() == 0}
 * @param securityScoreBreakdown            localized factor list mirroring {@code securityScore}; {@code null} when unused
 */
public record PasswordSettingsOverviewResponse(
        List<OuPasswordPolicyDto> passwordPoliciesByOu,
        TwoStepVerificationDto twoStepVerification,
        List<PasswordChangeRequirementDto> usersWithForcedChange,
        PasswordSettingsSummaryDto summary,
        List<AdminWithSecurityKeyDto> adminsWithoutSecurityKeys,
        String adminsSecurityKeysErrorMessage,
        Integer securityScore,
        SecurityScoreBreakdownDto securityScoreBreakdown
) {
}
