package com.cloudmen.cloudguard.dto.licenses;

import java.util.List;

/**
 * A Data Transfer Object (DTO) representing the comprehensive payload for the license management page. <p>
 *
 * This record aggregates the current license distributions and a list of inactive licensed users. Additionally,
 * it includes specific metadata metrics to assist the frontend in rendering accurate and well-scaled visual charts.
 *
 * @param licenseTypes      the list of different license tiers or SKUs currently managed within the organization
 * @param inactiveUsers     the list of inactive users who are currently consuming valuable licenses
 * @param maxLicenseAmount  the highest count of licenses in a single category, typically used to determine the maximum
 *                          axis value for frontend charts
 * @param chartStepSize     the calculated numerical step interval for chart axes, ensuring clean and readable visual
 *                          scaling in the user interface
 */
public record LicensePageResponse(
        List<LicenseType> licenseTypes,
        List<InactiveUser> inactiveUsers,
        int maxLicenseAmount,
        int chartStepSize
) {
}
