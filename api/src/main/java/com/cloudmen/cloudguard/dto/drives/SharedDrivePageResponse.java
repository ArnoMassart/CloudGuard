package com.cloudmen.cloudguard.dto.drives;

import java.util.List;

/**
 * A Data Transfer Object (DTO) representing a paginated response for a list of Shared Drives. <p>
 *
 * This record encapsulates a subset of shared drives along with a token to fetch subsequent pages, facilitating the
 * efficient retrieval of large drive inventories without overwhelming the client or server.
 *
 * @param drives        the list of detailed shared drive information for the current page
 * @param nextPageToken the token used to request the next page of results, or {@code null} if there are no more pages
 */
public record SharedDrivePageResponse(
        List<SharedDriveBasicDetail> drives,
        String nextPageToken
) {
}
