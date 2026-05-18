package com.cloudmen.cloudguard.dto.password;

/**
 * Compact Directory-derived totals reused by password KPIs and the headline security score.
 *
 * @param usersWithForcedChange count with {@code changePasswordAtNextLogin}
 * @param usersWith2SvEnrolled  users enrolled in 2-step verification
 * @param usersWith2SvEnforced users enforced into 2SV per Directory flags
 * @param totalUsers            all synced users considered for the module
 */
public record PasswordSettingsSummaryDto(
        int usersWithForcedChange,
        int usersWith2SvEnrolled,
        int usersWith2SvEnforced,
        int totalUsers
) {
}
