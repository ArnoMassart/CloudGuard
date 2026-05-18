package com.cloudmen.cloudguard.dto.password;

/**
 * Generic password-rule projection (complexity, rotation, reuse). Not wired into {@link PasswordSettingsOverviewResponse}
 * today; kept as a stable shape for future endpoints or UI expansions.
 *
 * @param minLength             minimum password length if constrained
 * @param maxLength             maximum password length if constrained
 * @param requireLowercase      complexity gate
 * @param requireUppercase      complexity gate
 * @param requireNumeric        complexity gate
 * @param requireSpecialChar    complexity gate
 * @param expirationDays        rotation interval in days
 * @param reusePreventionCount  prior passwords disallowed
 * @param description           human-readable summary for tooling or exports
 */
public record PasswordPolicyDto(
        Integer minLength,
        Integer maxLength,
        Boolean requireLowercase,
        Boolean requireUppercase,
        Boolean requireNumeric,
        Boolean requireSpecialChar,
        Integer expirationDays,
        Integer reusePreventionCount,
        String description
) {
}
