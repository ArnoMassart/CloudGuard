package com.cloudmen.cloudguard.dto.groups;

import java.util.List;

/**
 * Value stored in {@link com.cloudmen.cloudguard.service.cache.GoogleGroupsCacheService}: full group list plus fetch time.
 *
 * @param allGroups complete workspace group list after last successful sync
 * @param timestamp epoch millis when this entry was produced ({@link System#currentTimeMillis()})
 */
public record GroupCacheEntry(List<CachedGroupItem> allGroups, long timestamp) {}
