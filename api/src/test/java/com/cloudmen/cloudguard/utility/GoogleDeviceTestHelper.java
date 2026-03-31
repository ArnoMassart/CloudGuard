package com.cloudmen.cloudguard.utility;

import com.cloudmen.cloudguard.dto.devices.DeviceCacheEntry;
import com.cloudmen.cloudguard.service.cache.GoogleDeviceCacheService;
import com.google.api.client.util.DateTime;
import com.google.api.services.admin.directory.model.ChromeOsDevice;
import com.google.api.services.admin.directory.model.MobileDevice;
import com.google.api.services.cloudidentity.v1.model.GoogleAppsCloudidentityDevicesV1Device;
import com.google.api.services.cloudidentity.v1.model.GoogleAppsCloudidentityDevicesV1DeviceUser;

import java.util.List;

import static com.cloudmen.cloudguard.utility.GlobalTestHelper.ADMIN;
import static org.mockito.Mockito.*;

public class GoogleDeviceTestHelper {
    public static MobileDevice createMobileDevice(
            String resourceId, String email, String os, String status,
            String passwordStatus, String encryptionStatus, String compromisedStatus, DateTime lastSync) {
        MobileDevice device = new MobileDevice();
        device.setResourceId(resourceId);
        device.setName(List.of(email));
        device.setOs(os);
        device.setStatus(status);
        device.setDevicePasswordStatus(passwordStatus);
        device.setEncryptionStatus(encryptionStatus);
        device.setDeviceCompromisedStatus(compromisedStatus);
        device.setLastSync(lastSync);
        return device;
    }

    public static ChromeOsDevice createChromeOsDevice(
            String deviceId, String email, String osVersion, String status, DateTime lastSync) {
        ChromeOsDevice device = new ChromeOsDevice();
        device.setDeviceId(deviceId);
        device.setOsVersion(osVersion);
        device.setStatus(status);
        device.setLastSync(lastSync);

        ChromeOsDevice.RecentUsers user = new ChromeOsDevice.RecentUsers();
        user.setEmail(email);
        device.setRecentUsers(List.of(user));
        return device;
    }

    public static DeviceCacheEntry.EndpointWrapper createEndpointWrapper(
            String name, String type, String email, String osVersion, String encryptionState,
            String compromisedState, String lastSyncTimeIso) {

        GoogleAppsCloudidentityDevicesV1Device device = new GoogleAppsCloudidentityDevicesV1Device();
        device.setName(name);
        device.setDeviceType(type);
        device.setOsVersion(osVersion);
        device.setEncryptionState(encryptionState);
        device.setCompromisedState(compromisedState);
        device.setLastSyncTime(lastSyncTimeIso);

        GoogleAppsCloudidentityDevicesV1DeviceUser user = new GoogleAppsCloudidentityDevicesV1DeviceUser();
        user.setUserEmail(email);
        user.setCompromisedState(compromisedState);

        return new DeviceCacheEntry.EndpointWrapper(device, List.of(user));
    }

    public static void mockCacheEntry(
            GoogleDeviceCacheService cacheService,
            List<MobileDevice> mobiles,
            List<ChromeOsDevice> chromeOs,
            List<DeviceCacheEntry.EndpointWrapper> endpoints) {
        DeviceCacheEntry mockEntry = mock(DeviceCacheEntry.class);

        lenient().when(mockEntry.mobileDevices()).thenReturn(mobiles);
        lenient().when(mockEntry.chromeOsDevices()).thenReturn(chromeOs);
        lenient().when(mockEntry.endpointDevices()).thenReturn(endpoints);

        when(cacheService.getOrFetchDeviceData(ADMIN)).thenReturn(mockEntry);
    }
}
