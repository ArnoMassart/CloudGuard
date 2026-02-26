package com.cloudmen.cloudguard.dto.organization;

import com.google.api.services.admin.directory.model.OrgUnit;

import java.util.List;
import java.util.Map;

public record OrgUnitCacheEntry(
        List<OrgUnit> allOrgUnits,
        Map<String, Integer> userCounts,
        long timestamp
) {
}
