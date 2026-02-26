package com.cloudmen.cloudguard.dto.drives;

import com.google.api.services.drive.model.Drive;

import java.util.List;

public record SharedDriveCacheEntry(
        List<SharedDriveBasicDetail> allDrives,
        long timestamp
) {
}
