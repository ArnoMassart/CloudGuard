package com.cloudmen.cloudguard.dto.groups;

public record CachedGroupItem(
        String name,
        String email,
        GroupOrgDetail detail
) {
}
