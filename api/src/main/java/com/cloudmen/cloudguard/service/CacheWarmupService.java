package com.cloudmen.cloudguard.service;

import com.cloudmen.cloudguard.service.cache.GoogleUsersCacheService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.concurrent.CompletableFuture;

@Service
public class CacheWarmupService {

    private static final Logger log = LoggerFactory.getLogger(CacheWarmupService.class);

    private final GoogleUsersCacheService usersCacheService;
    private final GoogleGroupsService groupsService;
    private final GoogleSharedDriveService sharedDriveService;
    private final GoogleMobileDeviceService mobileDeviceService;

    public CacheWarmupService(GoogleUsersCacheService usersCacheService, GoogleGroupsService groupsService, GoogleSharedDriveService sharedDriveService, GoogleMobileDeviceService mobileDeviceService) {
        this.usersCacheService = usersCacheService;
        this.groupsService = groupsService;
        this.sharedDriveService = sharedDriveService;
        this.mobileDeviceService = mobileDeviceService;
    }

    public void warmupAllCachesAsync(String loggedInEmail) {
        CompletableFuture<Void> usersTask = CompletableFuture.runAsync(() -> {
            try { usersCacheService.forceRefreshCache(loggedInEmail); } catch (Exception e) { log.warn("Users warmup failed", e); }
        });

        CompletableFuture<Void> groupsTask = CompletableFuture.runAsync(() -> {
            try { groupsService.forceRefreshCache(loggedInEmail); } catch (Exception e) { log.warn("Groups warmup failed", e); }
        });

        CompletableFuture<Void> drivesTask = CompletableFuture.runAsync(() -> {
            try { sharedDriveService.forceRefreshCache(loggedInEmail); } catch (Exception e) { log.warn("Drives warmup failed", e); }
        });

        CompletableFuture<Void> devicesTask = CompletableFuture.runAsync(() -> {
            try { mobileDeviceService.forceRefreshCache(loggedInEmail); } catch (Exception e) { log.warn("Devices warmup failed", e); }
        });

        CompletableFuture.allOf(usersTask, groupsTask, drivesTask, devicesTask).thenAccept(v -> log.info("✅ Cache warm-up SUCCESVOL voltooid voor alle modules voor: {}", loggedInEmail));
    }
}
