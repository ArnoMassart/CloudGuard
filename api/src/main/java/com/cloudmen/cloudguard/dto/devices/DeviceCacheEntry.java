package com.cloudmen.cloudguard.dto.devices;

import com.google.api.services.admin.directory.model.ChromeOsDevice;
import com.google.api.services.admin.directory.model.MobileDevice;
import com.google.api.services.cloudidentity.v1.model.GoogleAppsCloudidentityDevicesV1Device;
import com.google.api.services.cloudidentity.v1.model.GoogleAppsCloudidentityDevicesV1DeviceUser;

import java.util.List;

/**
 * A Data Transfer Object (DTO) used to cache a comprehensive snapshot of an organization's device inventory. <p>
 *
 * This record aggregates different categories of devices retrieved from Google Workspace, including mobile devices,
 * Chrome OS devices, and general endpoints. It also includes a timestamp to facilitate cache expiration and renewal
 * logic.
 *
 * @param mobileDevices     a list of managed mobile devices
 * @param chromeOsDevices   a list of managed Chrome OS devices
 * @param endpointDevices   a list of endpoint devices wrapped with their associated user data
 * @param timestamp         the exact time (in milliseconds) when this cache entry was created
 */
public record DeviceCacheEntry(
        List<MobileDevice> mobileDevices,
        List<ChromeOsDevice> chromeOsDevices,
        List<EndpointWrapper> endpointDevices,
        long timestamp
) {

    /**
     * A wrapper record that logically groups a Cloud Identity endpoint device with its associated user accounts.
     *
     * @param device        the core device information retrieved from the Cloud Identity API
     * @param deviceUsers   a list of users associated with or logged into this specific device
     */
    public record EndpointWrapper(
            GoogleAppsCloudidentityDevicesV1Device device,
            List<GoogleAppsCloudidentityDevicesV1DeviceUser> deviceUsers
    ) {}
}
