package com.cloudmen.cloudguard.dto.adminsecuritykeys;

public record AdminWithSecurityKeyDto(
        String id,
        String name,
        String email,
        String role,
        String orgUnitPath,
        boolean twoFactorEnabled,
        int numSecurityKeys,
        int numPasskeysEnrolled
) {
}
