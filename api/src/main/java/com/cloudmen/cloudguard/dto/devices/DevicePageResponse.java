package com.cloudmen.cloudguard.dto.devices;

import java.util.List;

public record DevicePageResponse(
        List<DeviceDetail> devices,
        String nextPageToken
) {
}
