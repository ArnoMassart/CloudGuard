package com.cloudmen.cloudguard.dto.password;

public record PasswordSettingsSummaryDto(
        int usersWithForcedChange,
        int usersWith2SvEnrolled,
        int usersWith2SvEnforced,
        int totalUsers
) {
}
