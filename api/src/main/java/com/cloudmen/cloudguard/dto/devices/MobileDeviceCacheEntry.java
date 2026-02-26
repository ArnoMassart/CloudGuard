package com.cloudmen.cloudguard.dto.devices;

import com.google.api.services.admin.directory.model.MobileDevice;

import java.util.List;

public record MobileDeviceCacheEntry(
        List<MobileDevice> allDevices,
        long timestamp
) {
}
