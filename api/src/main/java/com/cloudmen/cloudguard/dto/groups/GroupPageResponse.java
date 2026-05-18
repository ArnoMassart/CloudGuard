package com.cloudmen.cloudguard.dto.groups;

import java.util.List;

/**
 * Single page of group rows from {@code GET /google/groups}.
 *
 * @param groups          slice of {@link GroupOrgDetail} for this page (stable order from cache)
 * @param nextPageToken   1-based index of the next page as a string, or {@code null} when there is no next page
 */
public record GroupPageResponse(List<GroupOrgDetail> groups, String nextPageToken) {}
