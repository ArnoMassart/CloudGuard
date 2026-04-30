package com.cloudmen.cloudguard.dto.licenses;

/**
 * A Data Transfer Object (DTO) representing a user who has been flagged as inactive within the organization. <p>
 *
 * This record is primarily used for license management and cost optimization, helping administrators identify accounts
 * that are consuming valuable licenses buy have not been accessed for an extended period.
 *
 * @param email                 the primary email address of the user
 * @param lastLogin             a formatted string indicating the date and time of the user's last successful login
 * @param licenseType           the specific Google Workspace license SKU or tier currently assigned to the user
 * @param isTwoFactorEnabled    {@code true} if the user has two-factor authentication (2FA) configured and active
 * @param daysInactive          the total number of days that have elapsed since the user's last recorded login
 */
public record InactiveUser(
        String email,
        String lastLogin,
        String licenseType,
        boolean isTwoFactorEnabled,
        long daysInactive
) {
}
