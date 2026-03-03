package com.cloudmen.cloudguard.service.cache;

import com.cloudmen.cloudguard.dto.licenses.LicenseCacheEntry;
import com.cloudmen.cloudguard.service.policy.PolicyApiCacheService;
import com.cloudmen.cloudguard.service.policy.TSVPolicyProvider;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.concurrent.CompletableFuture;

@Service
public class CacheWarmupService {

    private static final Logger log = LoggerFactory.getLogger(CacheWarmupService.class);

    private final GoogleUsersCacheService usersCacheService;
    private final GoogleGroupsCacheService groupsCacheService;
    private final GoogleOrgUnitCacheService orgUnitCacheService;
    private final GoogleSharedDriveCacheService sharedDriveCacheService;
    private final GoogleMobileDeviceCacheService mobileDeviceCacheService;

    private final TSVPolicyProvider tsvPolicyProvider;
    private final PolicyApiCacheService policyApiCacheService;

    private final GoogleLicenseCacheService licenseCacheService;

    public CacheWarmupService(GoogleUsersCacheService usersCacheService, GoogleGroupsCacheService groupsCacheService, GoogleOrgUnitCacheService orgUnitCacheService, GoogleSharedDriveCacheService sharedDriveCacheService, GoogleMobileDeviceCacheService mobileDeviceCacheService, TSVPolicyProvider tsvPolicyProvider, PolicyApiCacheService policyApiCacheService, GoogleLicenseCacheService licenseCacheService) {
        this.usersCacheService = usersCacheService;
        this.groupsCacheService = groupsCacheService;
        this.orgUnitCacheService = orgUnitCacheService;
        this.sharedDriveCacheService = sharedDriveCacheService;
        this.mobileDeviceCacheService = mobileDeviceCacheService;
        this.tsvPolicyProvider = tsvPolicyProvider;
        this.policyApiCacheService = policyApiCacheService;
        this.licenseCacheService = licenseCacheService;
    }

    public void warmupAllCachesAsync(String loggedInEmail) {
        CompletableFuture<Void> usersTask = CompletableFuture.runAsync(() -> {
            try { usersCacheService.forceRefreshCache(loggedInEmail); } catch (Exception e) { log.warn("Users warmup failed", e); }
        });

        CompletableFuture<Void> groupsTask = CompletableFuture.runAsync(() -> {
            try { groupsCacheService.forceRefreshCache(loggedInEmail); } catch (Exception e) { log.warn("Groups warmup failed", e); }
        });

        CompletableFuture<Void> orgUnitsTask = CompletableFuture.runAsync(() -> {
            try { orgUnitCacheService.forceRefreshCache(loggedInEmail); } catch (Exception e) { log.warn("Org units warmup failed", e); }
        });

        CompletableFuture<Void> drivesTask = CompletableFuture.runAsync(() -> {
            try { sharedDriveCacheService.forceRefreshCache(loggedInEmail); } catch (Exception e) { log.warn("Drives warmup failed", e); }
        });

        CompletableFuture<Void> devicesTask = CompletableFuture.runAsync(() -> {
            try { mobileDeviceCacheService.forceRefreshCache(loggedInEmail); } catch (Exception e) { log.warn("Devices warmup failed", e); }
        });

        CompletableFuture<Void> tsvTask = CompletableFuture.runAsync(() -> {
            try { tsvPolicyProvider.forceRefreshCache(loggedInEmail); } catch (Exception e) { log.warn("TSV Policy warmup failed", e); }
        });

        CompletableFuture<Void> policyApiTask = CompletableFuture.runAsync(() -> {
            try {
                policyApiCacheService.getAllPolicies(loggedInEmail);
                policyApiCacheService.getOuIdToPathMap(loggedInEmail);
            } catch (Exception e) { log.warn("Policy API warmup failed", e); }
        });

        CompletableFuture<Void> licensesTask = CompletableFuture.runAsync(() -> {
            try { licenseCacheService.forceRefreshCache(loggedInEmail); } catch (Exception e) { log.warn("Licenses warmup failed", e); }
        });


        CompletableFuture.allOf(usersTask, groupsTask, orgUnitsTask, drivesTask, devicesTask, tsvTask, policyApiTask, licensesTask).thenAccept(v -> log.info("✅ Cache warm-up succesvol voltooid voor alle modules voor: {}", loggedInEmail));
    }
}
