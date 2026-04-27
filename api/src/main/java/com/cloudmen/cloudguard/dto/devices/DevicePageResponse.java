package com.cloudmen.cloudguard.dto.devices;

import java.util.List;

/**
 * A Data Transfer Object (DTO) representing a paginated response for a list of devices. <p>
 *
 * This record encapsulates a subset of devices along with a token to fetch subsequent pages, facilitating the
 * efficient retrieval of large device inventories without overwhelming the client or server.
 *
 * @param devices       the list of detailed device information for the current page
 * @param nextPageToken the token used to request the next page of results, or {@code null} if there are no more pages
 */
public record DevicePageResponse(
        List<DeviceDetail> devices,
        String nextPageToken
) {
}
