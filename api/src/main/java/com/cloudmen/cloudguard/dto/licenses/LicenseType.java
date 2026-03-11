package com.cloudmen.cloudguard.dto.licenses;

public record LicenseType(
        String skuId,
        String skuName,
        int totalAssigned
) {
}
