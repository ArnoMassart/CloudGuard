package com.cloudmen.cloudguard.dto.licenses;

/**
 * A Data Transfer Object (DTO) providing a high-level summary of the organization's license distribution and usage. <p>
 *
 * This record encapsulates key metrics such as total license assignments, the number of unused or available licenses,
 * and accounts flagged as risky (e.g., inactive users consuming paid licenses), aiding administrators in cost
 * optimization and license management.
 *
 * @param totalAssigned     the total number of licenses currently assigned to users within the organization
 * @param riskyAccounts     the number of licensed accounts flagged as risky or underutilized (e.g., inactive users)
 * @param unusedLicenses    the number of purchased licenses that are currently unassigned and available for allocation
 */
public record LicenseOverviewResponse(
        int totalAssigned,
        int riskyAccounts,
        int unusedLicenses
) {
}
