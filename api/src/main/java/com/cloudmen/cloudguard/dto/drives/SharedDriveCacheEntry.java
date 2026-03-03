package com.cloudmen.cloudguard.dto.drives;

import java.util.List;

public record SharedDriveCacheEntry(
        List<SharedDriveBasicDetail> allDrives,
        long timestamp
) {
}
