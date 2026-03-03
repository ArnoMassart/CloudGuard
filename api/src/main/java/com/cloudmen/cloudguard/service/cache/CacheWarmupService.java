package com.cloudmen.cloudguard.service.cache;

import com.cloudmen.cloudguard.service.AppPasswordsService;
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
    private final AppPasswordsService appPasswordsService;
    private final GoogleOAuthCacheService oAuthCacheService;

    private final TSVPolicyProvider tsvPolicyProvider;
    private final PolicyApiCacheService policyApiCacheService;

    public CacheWarmupService(GoogleUsersCacheService usersCacheService, GoogleGroupsCacheService groupsCacheService, GoogleOrgUnitCacheService orgUnitCacheService, GoogleSharedDriveCacheService sharedDriveCacheService, GoogleMobileDeviceCacheService mobileDeviceCacheService, AppPasswordsService appPasswordsService, GoogleOAuthCacheService oAuthCacheService, TSVPolicyProvider tsvPolicyProvider, PolicyApiCacheService policyApiCacheService) {
        this.usersCacheService = usersCacheService;
        this.groupsCacheService = groupsCacheService;
        this.orgUnitCacheService = orgUnitCacheService;
        this.sharedDriveCacheService = sharedDriveCacheService;
        this.mobileDeviceCacheService = mobileDeviceCacheService;
        this.appPasswordsService = appPasswordsService;
        this.oAuthCacheService = oAuthCacheService;
        this.tsvPolicyProvider = tsvPolicyProvider;
        this.policyApiCacheService = policyApiCacheService;
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

        CompletableFuture<Void> appPasswordsTask = CompletableFuture.runAsync(() -> {
            try { appPasswordsService.forceRefreshCache(loggedInEmail); } catch (Exception e) { log.warn("App passwords warmup failed", e); }
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

        CompletableFuture<Void> oAuthTask = CompletableFuture.runAsync(() -> {
            try { oAuthCacheService.forceRefreshCache(loggedInEmail); } catch (Exception e) { log.warn("OAuth warmup failed", e); }
        });

        CompletableFuture.allOf(usersTask, groupsTask, orgUnitsTask, drivesTask, devicesTask, appPasswordsTask, tsvTask, policyApiTask).thenAccept(v -> log.info("✅ Cache warm-up succesvol voltooid voor alle modules voor: {}", loggedInEmail));
    }
}
