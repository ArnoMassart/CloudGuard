package com.cloudmen.cloudguard.dto.password;

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
