package com.cloudmen.cloudguard.dto.licenses;

public record LicenseOverviewResponse(
        long totalAssigned,
        long riskyAccounts,
        long unusedLicenses,
        long mfaPercentage
) {
}
