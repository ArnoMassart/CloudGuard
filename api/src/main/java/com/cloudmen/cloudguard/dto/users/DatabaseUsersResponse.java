package com.cloudmen.cloudguard.dto.users;

import java.util.List;

/**
 * A Data Transfer Object (DTO) representing a paginated response for a list of users. <p>
 *
 * This record encapsulates a subset of user data for the current page and includes a token for fetching the next set
 * of results. This supports efficient data loading and smooth navigation in the user interface when dealing with large
 * user sets.
 *
 * @param users         the list of {@link UserDto} objects retrieved for the current page
 * @param nextPageToken the token used to request the next page of results, or {@code null} if no additional results
 *                      are available
 */
public record DatabaseUsersResponse(
        List<UserDto> users,
        String nextPageToken
) {
}
