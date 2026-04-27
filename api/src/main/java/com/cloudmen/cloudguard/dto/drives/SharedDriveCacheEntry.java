package com.cloudmen.cloudguard.dto.drives;

import java.util.List;

/**
 * A Data Transfer Object (DTO) used to cache a comprehensive list of Shared Drives. <p>
 *
 * This record encapsulates a collection of shared drives along with a timestamp, which is utilized by the
 * caching mechanism to determine the data's freshness and expiration.
 *
 * @param allDrives the list of detailed shared drive records currently held in the cache
 * @param timestamp the exact time (in milliseconds) when this cache entry was created
 */
public record SharedDriveCacheEntry(
        List<SharedDriveBasicDetail> allDrives,
        long timestamp
) {
}
