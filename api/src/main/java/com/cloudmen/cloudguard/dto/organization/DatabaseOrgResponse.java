package com.cloudmen.cloudguard.dto.organization;

import com.cloudmen.cloudguard.domain.model.Organization;
import java.util.List;

/**
 * A Data Transfer Object (DTO) representing a paginated response for a list of organizations. <p>
 *
 * This record encapsulates a subset of organizations retrieved from the database for the current page, along with a
 * token to fetch subsequent pages.
 *
 * @param organizations the list of {@link Organization} entities retrieved for the current page of results
 * @param nextPageToken the token used to request the next page of results, or {@code null} if there are no more pages
 */
public record DatabaseOrgResponse(
        List<Organization> organizations,
        String nextPageToken
) {
}
