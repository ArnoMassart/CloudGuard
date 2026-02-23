package com.cloudmen.cloudguard.dto.devices;

import java.util.List;

public record MobileDevicePageResponse(
        List<MobileDeviceDetail> devices,
        String nextPageToken
) {
}
