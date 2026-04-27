package com.cloudmen.cloudguard.dto.licenses;

import java.util.List;

/**
 * A Data Transfer Object (DTO) used to cache a comprehensive snapshot of the organization's license utilization. <p>
 *
 * This record aggregates lists of available license types and inactive users who are currently consuming
 * those licenses. It also includes a timestamp to facilitate cache expiration and renewal logic, ensuring data
 * freshness.
 *
 * @param licenseTypes  a list of the different Google Workspace license SKUs or tiers present within the organization
 * @param inactiveUsers a list of users who are occupying licenses but have been flagged as inactive based on their
 *                      last login
 * @param timestamp     the exact time (in milliseconds) when this cache entry was created
 */
public record LicenseCacheEntry(
        List<LicenseType> licenseTypes,
        List<InactiveUser> inactiveUsers,
        long timestamp
) {
}
