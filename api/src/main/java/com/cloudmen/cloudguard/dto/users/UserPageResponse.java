package com.cloudmen.cloudguard.dto.users;

import java.util.List;

/**
 * A Data Transfer Object (DTO) representing a paginated response for a list of detailed organizational users. <p>
 *
 * This record encapsulates a subset of user details and security metrics for the current page, along with a token to
 * fetch subsequent pages. This structure facilitates efficient data retrieval and smooth pagination within the user
 * interface.
 *
 * @param users         the list of {@link UserOrgDetail} objects retrieved for the current page of results
 * @param nextPageToken the token used to request the next page of results, or {@code null} if no additional pages
 *                      are available
 */
public record UserPageResponse(
        List<UserOrgDetail> users,
        String nextPageToken
) {}
