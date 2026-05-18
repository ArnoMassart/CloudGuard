package com.cloudmen.cloudguard.dto.domain;

import java.util.List;

/**
 * Cached snapshot of {@link DomainDto} rows for a tenant, stored by {@link com.cloudmen.cloudguard.service.cache.GoogleDomainCacheService}.
 *
 * @param domains   flattened primary/secondary domains plus aliases
 * @param timestamp epoch millis when this entry was produced at Google
 */
public record DomainCacheEntry(
        List<DomainDto> domains,
        long timestamp
) {
}
