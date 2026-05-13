package com.cloudmen.cloudguard.dto.organization;

import com.google.api.services.admin.directory.model.OrgUnit;

import java.util.List;
import java.util.Map;

/**
 * Snapshot stored in {@link com.cloudmen.cloudguard.service.cache.GoogleOrgUnitCacheService}: full Admin SDK OU list
 * plus user population counts per Directory path, with a cache timestamp.
 *
 * @param allOrgUnits flat list of {@link OrgUnit} rows from {@code orgunits.list}
 * @param userCounts  keys are {@link OrgUnit#getOrgUnitPath()}-style strings (including {@code "/"}); values are user totals from {@code users.list}
 * @param timestamp   epoch millis when this entry was written ({@link System#currentTimeMillis()})
 */
public record OrgUnitCacheEntry(List<OrgUnit> allOrgUnits, Map<String, Integer> userCounts, long timestamp) {}
