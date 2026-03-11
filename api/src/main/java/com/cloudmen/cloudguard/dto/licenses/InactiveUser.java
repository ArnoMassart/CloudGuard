package com.cloudmen.cloudguard.dto.licenses;

public record InactiveUser(
        String email,
        String lastLogin,
        String licenseType,
        boolean isTwoFactorEnabled,
        long daysInactive
) {
}
