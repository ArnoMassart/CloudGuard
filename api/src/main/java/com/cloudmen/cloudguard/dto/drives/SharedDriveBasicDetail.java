package com.cloudmen.cloudguard.dto.drives;

import com.google.api.client.util.DateTime;

/**
 * A Data Transfer Object (DTO) representing the fundamental details and security settings of a Shared Drive. <p>
 *
 * This record encapsulates basic information such as the drive's identity, membership counts (including external
 * exposure), creation timestamps, access restrictions, and an assessed security risk level.
 *
 * @param id                        the unique identifier for the shared drive
 * @param name                      the display name of the shared drive
 * @param totalMembers              the total number of users or groups with access to the drive
 * @param externalMembers           the number of members residing outside the organization's domain
 * @param totalOrganizers           the number of members holding organizer (administrative) privileges
 * @param createdTime               the raw API timestamp indicating when the drive was created
 * @param parsedTime                a formatted, human-readable string representing the creation time
 * @param onlyDomainUsersAllowed    {@code true} if access is strictly limited to internal domain users
 * @param onlyMembersCanAccess      {@code true} if files cannot be shared with non-members via links
 * @param risk                      a descriptive string or classification indicating the drive's security risk
 */
public record SharedDriveBasicDetail(
        String id,
        String name,
        int totalMembers,
        int externalMembers,
        int totalOrganizers,
        DateTime createdTime,
        String parsedTime,
        boolean onlyDomainUsersAllowed,
        boolean onlyMembersCanAccess,
        String risk
) {
}
