package com.cloudmen.cloudguard.dto.groups;

import java.util.List;

public record GroupCacheEntry(
        List<CachedGroupItem> allGroups,
        long timestamp
) {
}
