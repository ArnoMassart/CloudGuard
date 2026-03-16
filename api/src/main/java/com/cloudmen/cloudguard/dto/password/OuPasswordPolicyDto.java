package com.cloudmen.cloudguard.dto.password;

/**
 * Password policy for a specific Organizational Unit.
 * Matches the Admin Console layout: minimale lengte, wachtwoordverloop,
 * sterke wachtwoorden, wachtwoordgeschiedenis.
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
