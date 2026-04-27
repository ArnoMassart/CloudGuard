package com.cloudmen.cloudguard.dto.oauth;

import java.util.List;

/**
 * A Data Transfer Object (DTO) representing a paginated response for a list of authorized OAuth applications. <p>
 *
 * This record encapsulates a subset of aggregated applications for the current page alongside a token to fetch
 * subsequent pages. Additionally, it includes overall counts of filtered applications to assist the frontend in
 * rendering accurate pagination controls and dynamic risk-based filter badges.
 *
 * @param apps                  the list of aggregated application details for the current page of results
 * @param nextPageToken         the token used to request the next page of results, or {@code null} if there are no
 *                              more pages
 * @param allFilteredApps       the total number of applications that match the currently applied search and filter
 *                              criteria across all pages
 * @param allHighRiskApps       the total number of high-risk applications that match the currently applied criteria
 * @param allNotHighRiskApps    the total number of non-high-risk (e.g., low or medium risk) applications that match
 *                              the currently applied criteria
 */
public record OAuthPagedResponse(
        List<AggregatedAppDto> apps,
        String nextPageToken,
        int allFilteredApps,
        int allHighRiskApps,
        int allNotHighRiskApps
) {
}
