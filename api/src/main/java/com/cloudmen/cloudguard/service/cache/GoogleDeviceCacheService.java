package com.cloudmen.cloudguard.service.cache;

import com.cloudmen.cloudguard.dto.devices.DeviceCacheEntry;
import com.cloudmen.cloudguard.exception.GoogleWorkspaceSyncException;
import com.cloudmen.cloudguard.service.OrganizationService;
import com.cloudmen.cloudguard.service.UserService;
import com.cloudmen.cloudguard.utility.GoogleApiFactory;
import com.cloudmen.cloudguard.utility.GoogleServiceHelperMethods;
import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import com.google.api.services.admin.directory.Directory;
import com.google.api.services.admin.directory.DirectoryScopes;
import com.google.api.services.admin.directory.model.ChromeOsDevice;
import com.google.api.services.admin.directory.model.MobileDevice;
import com.google.api.services.cloudidentity.v1.CloudIdentity;
import com.google.api.services.cloudidentity.v1.CloudIdentityScopes;
import com.google.api.services.cloudidentity.v1.model.GoogleAppsCloudidentityDevicesV1ListDevicesResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;

@Service
public class GoogleDeviceCacheService {
    private static final Logger log = LoggerFactory.getLogger(GoogleDeviceCacheService.class);

    private final GoogleApiFactory googleApiFactory;
    private final UserService userService;
    private final OrganizationService organizationService;

    private final Cache<String, DeviceCacheEntry> cache = Caffeine.newBuilder()
            .expireAfterWrite(1, TimeUnit.HOURS)
            .maximumSize(100)
            .build();

    public GoogleDeviceCacheService(GoogleApiFactory googleApiFactory, UserService userService, OrganizationService organizationService) {
        this.googleApiFactory = googleApiFactory;
        this.userService = userService;
        this.organizationService = organizationService;
    }

    public void forceRefreshCache(String loggedInEmail) {
        cache.asMap().compute(loggedInEmail, this::fetchFromGoogle);
    }

    public DeviceCacheEntry getOrFetchDeviceData(String loggedInEmail) {
        return cache.get(loggedInEmail, email -> fetchFromGoogle(email, null));
    }

    private DeviceCacheEntry fetchFromGoogle(String loggedInEmail, DeviceCacheEntry fallbackEntry) {
        try {
            String adminEmail = GoogleServiceHelperMethods.getAdminEmailForUser(loggedInEmail, userService, organizationService);

            log.info("Ophalen LIVE Device data PARALLEL van Google. Gebruiker: {}, Impersonatie via Admin: {}", loggedInEmail, adminEmail);


            Set<String> scopes = Set.of(
                    DirectoryScopes.ADMIN_DIRECTORY_DEVICE_MOBILE_READONLY,
                    DirectoryScopes.ADMIN_DIRECTORY_DEVICE_CHROMEOS_READONLY
            );
            Directory directoryService = googleApiFactory.getDirectoryService(scopes, adminEmail);
            CloudIdentity cloudIdentityService = googleApiFactory.getCloudIdentityService(CloudIdentityScopes.CLOUD_IDENTITY_DEVICES_READONLY, adminEmail);

            CompletableFuture<List<MobileDevice>> mobileFuture = CompletableFuture.supplyAsync(() -> {
                try { return fetchRawMobileDevices(directoryService); }
                catch (Exception e) { log.error("Fout bij ophalen mobiele apparaten: {}", e.getMessage()); return new ArrayList<>(); }
            });

            CompletableFuture<List<ChromeOsDevice>> chromeOsFuture = CompletableFuture.supplyAsync(() -> {
                try { return fetchRawChromeOsDevices(directoryService); }
                catch (Exception e) { log.error("Fout bij ophalen ChromeOS: {}", e.getMessage()); return new ArrayList<>(); }
            });

            CompletableFuture<List<DeviceCacheEntry.EndpointWrapper>> endpointFuture = CompletableFuture.supplyAsync(() -> {
                try { return fetchRawEndpointDevices(cloudIdentityService); }
                catch (Exception e) { log.error("Fout bij ophalen Windows/Mac: {}", e.getMessage()); return new ArrayList<>(); }
            });

            CompletableFuture.allOf(mobileFuture, chromeOsFuture, endpointFuture).join();

            return new DeviceCacheEntry(
                    mobileFuture.get(),
                    chromeOsFuture.get(),
                    endpointFuture.get(),
                    System.currentTimeMillis()
            );

        } catch (Exception e) {
            if (fallbackEntry != null) { return fallbackEntry; }
            throw new GoogleWorkspaceSyncException("Fout bij ophalen Google Devices: " + e.getMessage());
        }
    }

    private List<MobileDevice> fetchRawMobileDevices(Directory directoryService) throws Exception {
        Map<String, MobileDevice> uniqueDevices = new LinkedHashMap<>();
        String pageToken = null;

        do {
            var request = directoryService.mobiledevices().list("my_customer")
                    .setPageToken(pageToken).setProjection("FULL").setMaxResults(100);
            var result = request.execute();

            if (result.getMobiledevices() != null) {
                for (MobileDevice d : result.getMobiledevices()) {
                    if (d.getResourceId() != null) {
                        uniqueDevices.put(d.getResourceId(), d);
                    }
                }
            }
            pageToken = result.getNextPageToken();
        } while (pageToken != null);

        return new ArrayList<>(uniqueDevices.values());
    }

    private List<ChromeOsDevice> fetchRawChromeOsDevices(Directory directoryService) throws Exception {
        Map<String, ChromeOsDevice> uniqueDevices = new LinkedHashMap<>();
        String pageToken = null;

        do {
            var request = directoryService.chromeosdevices().list("my_customer")
                    .setPageToken(pageToken).setProjection("FULL").setMaxResults(100);
            var result = request.execute();

            if (result.getChromeosdevices() != null) {
                for (ChromeOsDevice d : result.getChromeosdevices()) {
                    if (d.getDeviceId() != null) {
                        uniqueDevices.put(d.getDeviceId(), d);
                    }
                }
            }
            pageToken = result.getNextPageToken();
        } while (pageToken != null);

        return new ArrayList<>(uniqueDevices.values());
    }

    private List<DeviceCacheEntry.EndpointWrapper> fetchRawEndpointDevices(CloudIdentity cloudIdentity) throws Exception {
        Map<String, DeviceCacheEntry.EndpointWrapper> uniqueEndpoints = new LinkedHashMap<>();
        String pageToken = null;

        do {
            var request = cloudIdentity.devices().list()
                    .setCustomer("customers/my_customer")
                    .setPageToken(pageToken)
                    .setPageSize(100);

            GoogleAppsCloudidentityDevicesV1ListDevicesResponse result = request.execute();

            if (result.getDevices() != null) {
                for (var d : result.getDevices()) {
                    String type = d.getDeviceType();
                    if ("ANDROID".equalsIgnoreCase(type) || "IOS".equalsIgnoreCase(type) || "CHROME_OS".equalsIgnoreCase(type)) {
                        continue;
                    }

                    // Lijst om de ruwe gebruikers in op te slaan
                    List<com.google.api.services.cloudidentity.v1.model.GoogleAppsCloudidentityDevicesV1DeviceUser> users = new ArrayList<>();

                    try {
                        var usersResult = cloudIdentity.devices().deviceUsers().list(d.getName())
                                .setCustomer("customers/my_customer")
                                .execute();

                        if (usersResult.getDeviceUsers() != null) {
                            users.addAll(usersResult.getDeviceUsers());
                        }
                    } catch (Exception e) {
                        log.warn("Kon gebruikers voor endpoint {} niet ophalen", d.getName());
                    }

                    // Stop het apparaat en de gebruikers samen in de wrapper en voeg toe aan de lijst
                    if (d.getName() != null) {
                        uniqueEndpoints.put(d.getName(), new DeviceCacheEntry.EndpointWrapper(d, users));
                    }
                }
            }
            pageToken = result.getNextPageToken();
        } while (pageToken != null);

        return new ArrayList<>(uniqueEndpoints.values());
    }
}