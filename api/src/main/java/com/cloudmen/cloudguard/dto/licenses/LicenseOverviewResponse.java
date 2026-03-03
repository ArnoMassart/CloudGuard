package com.cloudmen.cloudguard.dto.licenses;

public record LicenseOverviewResponse(
        long totalLicenses,
        long usagePercentage
) {
}
