package com.cloudmen.cloudguard.dto.licenses;

import java.util.List;

public record LicensePageResponse(
        List<LicenseType> licenseTypes,
        List<InactiveUser> inactiveUsers,
        MfaStats mfaStats,
        int maxLicenseAmount,
        int chartStepSize
) {
}
