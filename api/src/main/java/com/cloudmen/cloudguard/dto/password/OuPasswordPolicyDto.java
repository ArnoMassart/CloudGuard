package com.cloudmen.cloudguard.dto.password;

/**
 * Password policy row for one organizational unit, aligned with Admin Console themes (length, rotation,
 * strength, reuse) plus lightweight scoring for OU-level badges.
 *
 * @param orgUnitPath             Directory path (e.g. {@code /Sales/EMEA})
 * @param orgUnitName             trailing segment or {@code Root} for {@code /}
 * @param userCount               users attributed to this path for weighting
 * @param score                   heuristic 0–100 from {@link com.cloudmen.cloudguard.service.PasswordSettingsService}
 * @param problemCount            count of weak signals for badges
 * @param minLength               Policy minimum length if defined
 * @param expirationDays          rotation interval converted from Policy duration (0 means effectively none)
 * @param strongPasswordRequired {@code true} when Admin SDK mandates {@code STRONG}
 * @param reusePreventionCount    derived reuse posture ({@code 0} often means reuse allowed; {@code 1}+ prevention)
 * @param inherited               {@code false} when the matched policy OU equals this row’s OU
 */
public record OuPasswordPolicyDto(
        String orgUnitPath,
        String orgUnitName,
        int userCount,
        int score,
        int problemCount,
        Integer minLength,
        Integer expirationDays,
        Boolean strongPasswordRequired,
        Integer reusePreventionCount,
        boolean inherited
) {
}
