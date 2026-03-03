package com.cloudmen.cloudguard.dto.licenses;

import java.util.List;


public record LicenseCacheEntry(
        List<LicenseType> licenseTypes,
        List<InactiveUser> inactiveUsers,
        long timestamp
) {
}
