package com.cloudmen.cloudguard.dto.domain;

import java.util.List;

public record DomainCacheEntry(
        List<DomainDto> domains,
        long timestamp
) {
}
