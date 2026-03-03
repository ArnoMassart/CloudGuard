package com.cloudmen.cloudguard.dto;

import java.util.List;

public record DomainCacheEntry(
        List<DomainDto> domains,
        long timestamp
) {
}
