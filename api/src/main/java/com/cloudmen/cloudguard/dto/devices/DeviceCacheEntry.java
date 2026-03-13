package com.cloudmen.cloudguard.dto.devices;

import com.google.api.services.admin.directory.model.ChromeOsDevice;
import com.google.api.services.admin.directory.model.MobileDevice;
import com.google.api.services.cloudidentity.v1.model.GoogleAppsCloudidentityDevicesV1Device;
import com.google.api.services.cloudidentity.v1.model.GoogleAppsCloudidentityDevicesV1DeviceUser;

import java.util.List;

public record DeviceCacheEntry(
        List<MobileDevice> mobileDevices,
        List<ChromeOsDevice> chromeOsDevices,
        List<EndpointWrapper> endpointDevices,
        long timestamp
) {
    public record EndpointWrapper(
            GoogleAppsCloudidentityDevicesV1Device device,
            List<GoogleAppsCloudidentityDevicesV1DeviceUser> deviceUsers
    ) {}
}
