package com.cloudmen.cloudguard.service.cache;

import com.cloudmen.cloudguard.dto.devices.MobileDeviceCacheEntry;
import com.cloudmen.cloudguard.utility.GoogleApiFactory;
import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import com.google.api.client.util.DateTime;
import com.google.api.services.admin.directory.Directory;
import com.google.api.services.admin.directory.DirectoryScopes;
import com.google.api.services.admin.directory.model.MobileDevice;
import com.google.api.services.admin.directory.model.MobileDevices;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;

@Service
public class GoogleMobileDeviceCacheService {
    private static final Logger log = LoggerFactory.getLogger(GoogleMobileDeviceCacheService.class);

    private final GoogleApiFactory googleApiFactory;

    private final Cache<String, MobileDeviceCacheEntry> cache = Caffeine.newBuilder()
            .expireAfterWrite(1, TimeUnit.HOURS)
            .maximumSize(100)
            .build();

    public GoogleMobileDeviceCacheService(GoogleApiFactory googleApiFactory) {
        this.googleApiFactory = googleApiFactory;
    }

    public void forceRefreshCache(String loggedInEmail) {
        cache.asMap().compute(loggedInEmail, this::fetchFromGoogle);
    }

    public MobileDeviceCacheEntry getOrFetchDeviceData(String loggedInEmail, boolean isTestMode) {
        if (isTestMode) {
            return new MobileDeviceCacheEntry(createMockGoogleDevices(), System.currentTimeMillis());
        }

        return cache.get(loggedInEmail, email -> fetchFromGoogle(email, null));
    }

    private MobileDeviceCacheEntry fetchFromGoogle(String loggedInEmail, MobileDeviceCacheEntry fallbackEntry) {
        try {
            log.info("Ophalen LIVE Mobile Device data van Google voor: {}", loggedInEmail);
            Directory directoryService = googleApiFactory.getDirectoryService(
                    DirectoryScopes.ADMIN_DIRECTORY_DEVICE_MOBILE_READONLY, loggedInEmail);

            List<MobileDevice> allDevices = new ArrayList<>();
            String pageToken = null;

            do {
                Directory.Mobiledevices.List request = directoryService.mobiledevices().list("my_customer")
                        .setPageToken(pageToken)
                        .setProjection("FULL")
                        .setMaxResults(100);

                MobileDevices result = request.execute();

                if (result.getMobiledevices() != null) {
                    allDevices.addAll(result.getMobiledevices());
                }
                pageToken = result.getNextPageToken();
            } while (pageToken != null);

            return new MobileDeviceCacheEntry(allDevices, System.currentTimeMillis());

        } catch (Exception e) {
            if (fallbackEntry != null) {
                log.error("Google API faalde! Terugvallen op oude cache: {}", e.getMessage());
                return fallbackEntry;
            }
            throw new IllegalArgumentException("Fout bij ophalen Google Mobile Devices: " + e.getMessage());
        }
    }

    private List<MobileDevice> createMockGoogleDevices() {
        List<MobileDevice> list = new ArrayList<>();
        long now = System.currentTimeMillis();

        MobileDevice d1 = new MobileDevice();
        d1.setResourceId("dev-test-1");
        d1.setName(List.of("jan.jansen@bedrijf.nl"));
        d1.setModel("Samsung Galaxy S24");
        d1.setOs("Android 14");
        d1.setDevicePasswordStatus("PASSWORD_SET");
        d1.setEncryptionStatus("ENCRYPTED");
        d1.setDeviceCompromisedStatus("UNCOMPROMISED");
        d1.setStatus("APPROVED");
        d1.setLastSync(new DateTime(now - 86400000L));
        list.add(d1);

        MobileDevice d2 = new MobileDevice();
        d2.setResourceId("dev-test-2");
        d2.setName(List.of("pieter.devries@bedrijf.nl"));
        d2.setModel("Google Pixel 5");
        d2.setOs("Android 11");
        d2.setDevicePasswordStatus("NO_PASSWORD");
        d2.setEncryptionStatus("UNENCRYPTED");
        d2.setDeviceCompromisedStatus("UNCOMPROMISED");
        d2.setStatus("BLOCKED");
        d2.setLastSync(new DateTime(now - (86400000L * 30)));
        list.add(d2);

        MobileDevice d3 = new MobileDevice();
        d3.setResourceId("dev-test-3");
        d3.setName(List.of("lisa.smit@bedrijf.nl"));
        d3.setModel("iPhone 15 Pro");
        d3.setOs("iOS 17.2");
        d3.setDevicePasswordStatus("NO_PASSWORD");
        d3.setEncryptionStatus("ENCRYPTED");
        d3.setDeviceCompromisedStatus("COMPROMISED");
        d3.setStatus("APPROVED");
        d3.setLastSync(new DateTime(now - 3600000L));
        list.add(d3);

        MobileDevice d4 = new MobileDevice();
        d4.setResourceId("dev-test-4");
        d4.setName(List.of("mark.rutte@bedrijf.nl"));
        d4.setModel("iPhone 8");
        d4.setOs("iOS 15.0");
        d4.setDevicePasswordStatus("PASSWORD_SET");
        d4.setEncryptionStatus("ENCRYPTED");
        d4.setDeviceCompromisedStatus("UNCOMPROMISED");
        d4.setStatus("PENDING");
        d4.setLastSync(null);
        list.add(d4);

        MobileDevice d5 = new MobileDevice();
        d5.setResourceId("dev-test-5");
        d5.setName(List.of("emma.devries@bedrijf.nl"));
        d5.setModel("iPad Pro 11-inch");
        d5.setOs("iOS 16.5");
        d5.setDevicePasswordStatus("PASSWORD_SET");
        d5.setEncryptionStatus("ENCRYPTED");
        d5.setDeviceCompromisedStatus("COMPROMISED");
        d5.setStatus("BLOCKED");
        d5.setLastSync(new DateTime(now - 600000L));
        list.add(d5);

        MobileDevice d6 = new MobileDevice();
        d6.setResourceId("dev-test-6");
        d6.setName(List.of("tom.klaassen@bedrijf.nl"));
        d6.setModel("Motorola Edge");
        d6.setOs("Android 14");
        d6.setDevicePasswordStatus("PASSWORD_SET");
        d6.setEncryptionStatus("ENCRYPTING");
        d6.setDeviceCompromisedStatus("UNCOMPROMISED");
        d6.setStatus("APPROVED");
        d6.setLastSync(null);
        list.add(d6);

        // Copy all previous to have more mock data
        list.add(copyDevice(d5, "dev-test-7"));
        list.add(copyDevice(d2, "dev-test-8"));
        list.add(copyDevice(d4, "dev-test-9"));
        list.add(copyDevice(d6, "dev-test-10"));
        list.add(copyDevice(d3, "dev-test-11"));
        list.add(copyDevice(d4, "dev-test-12"));
        list.add(copyDevice(d1, "dev-test-13"));

        return list;
    }

    private MobileDevice copyDevice(MobileDevice original, String newId) {
        MobileDevice copy = new MobileDevice();
        copy.setResourceId(newId);
        copy.setName(original.getName());
        copy.setModel(original.getModel());
        copy.setOs(original.getOs());
        copy.setDevicePasswordStatus(original.getDevicePasswordStatus());
        copy.setEncryptionStatus(original.getEncryptionStatus());
        copy.setDeviceCompromisedStatus(original.getDeviceCompromisedStatus());
        copy.setStatus(original.getStatus());
        copy.setLastSync(original.getLastSync());
        return copy;
    }
}
