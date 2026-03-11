package com.cloudmen.cloudguard.dto.licenses;

public record LicenseOverviewResponse(
        int totalAssigned,
        int riskyAccounts,
        int unusedLicenses,
        int mfaPercentage
) {
}
