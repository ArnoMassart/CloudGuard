package com.cloudmen.cloudguard.dto.password;

public record PasswordChangeRequirementDto(
        String email,
        String fullName,
        String orgUnitPath,
        String reason
) {
}
