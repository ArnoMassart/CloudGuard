package com.cloudmen.cloudguard.dto.oauth;

import java.util.List;

public record AggregatedAppDto(
        String id,
        String name,
        String appType,
        String appSource,
        boolean isThirdParty,
        boolean isAnonymous,
        boolean isHighRisk,
        int totalUsers,
        int exposurePercentage,
        int scopeCount,
        List<DataAccessDto> dataAccess,
        int highRiskCount
) {
}
