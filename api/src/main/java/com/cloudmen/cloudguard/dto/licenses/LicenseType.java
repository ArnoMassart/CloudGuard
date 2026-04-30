package com.cloudmen.cloudguard.dto.licenses;

/**
 * A Data Transfer Object (DTO) representing a specific Google Workspace license tier and its current allocation
 * status. <p>
 *
 * This record encapsulates the identifying details of a license Stock Keeping Unit (SKU) alongside the total number
 * of users currently assigned to it within the organization.
 *
 * @param skuId         the unique alphanumeric Stock Keeping Unit identifier for the license
 * @param skuName       the human-readable display name or description of the license tier (e.g., Google Workspace
 *                      Enterprise Plus)
 * @param totalAssigned the total number of seats or users currently allocated to this specific license
 */
public record LicenseType(
        String skuId,
        String skuName,
        int totalAssigned
) {
}
