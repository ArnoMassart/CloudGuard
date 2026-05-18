package com.cloudmen.cloudguard.dto.password;

/**
 * Directory user required to change password at next login.
 *
 * @param email        primary mailbox
 * @param fullName     display name fallback to email when absent
 * @param orgUnitPath  OU placement for grouping in UI
 * @param reason       machine-readable trigger ({@code changePasswordAtNextLogin})
 */
public record PasswordChangeRequirementDto(
        String email,
        String fullName,
        String orgUnitPath,
        String reason
) {
}
