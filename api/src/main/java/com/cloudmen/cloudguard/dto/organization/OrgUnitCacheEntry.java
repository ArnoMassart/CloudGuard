package com.cloudmen.cloudguard.dto.organization;

import com.google.api.services.admin.directory.model.OrgUnit;

import java.util.List;
import java.util.Map;

/**
 * A Data Transfer Object (DTO) used to cache the organizational unit (OU) structure and associated user metrics. <p>
 *
 * This record holds a complete snapshot of the domain's OUs retrieved from Google Workspace, alongside a mapped count
 * of users residing in each unit. The inclusion of a timestamp facilitates effective cache expiration and data
 * freshness.
 *
 * @param allOrgUnits   the complete list of organizational units retrieved from the Google Workspace domain
 * @param userCounts    a mapping that associates each organizational unit (typically by its path or ID) with the total
 *                      number of users assigned to it
 * @param timestamp     the exact time (in milliseconds) when this cache entry was created
 */
public record OrgUnitCacheEntry(
        List<OrgUnit> allOrgUnits,
        Map<String, Integer> userCounts,
        long timestamp
) {
}
