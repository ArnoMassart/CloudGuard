package com.cloudmen.cloudguard.dto.licenses;

public record LicenseType(
        String skuId,
        String skuName,
        int totalPurchased,
        int totalAssigned,
        int totalAvailable
) {
}
