package com.cloudmen.cloudguard.dto.groups;

/**
 * One cached group: Directory display name and email together with enriched {@link GroupOrgDetail}.
 *
 * @param name   human-readable group name from Admin SDK (used for search/filter in {@link com.cloudmen.cloudguard.service.GoogleGroupsService})
 * @param email  canonical group address (member/settings API key)
 * @param detail persisted metrics and settings-derived flags for APIs and scoring
 */
public record CachedGroupItem(String name, String email, GroupOrgDetail detail) {}
