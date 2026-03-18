package com.cloudmen.cloudguard.service.cache;

import com.cloudmen.cloudguard.service.AdminSecurityKeysService;
import com.cloudmen.cloudguard.service.AppPasswordsService;
import com.cloudmen.cloudguard.service.PasswordSettingsService;
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
    private final GoogleDeviceCacheService mobileDeviceCacheService;
    private final AppPasswordsService appPasswordsService;
    private final AdminSecurityKeysService adminSecurityKeysService;
    private final GoogleOAuthCacheService oAuthCacheService;
    private final PasswordSettingsService passwordSettingsService;

    private final TSVPolicyProvider tsvPolicyProvider;
    private final PolicyApiCacheService policyApiCacheService;

    private final GoogleLicenseCacheService licenseCacheService;
    private final GoogleDomainCacheService domainCacheService;

    public CacheWarmupService(GoogleUsersCacheService usersCacheService, GoogleGroupsCacheService groupsCacheService, GoogleOrgUnitCacheService orgUnitCacheService, GoogleSharedDriveCacheService sharedDriveCacheService, GoogleDeviceCacheService mobileDeviceCacheService, AppPasswordsService appPasswordsService, AdminSecurityKeysService adminSecurityKeysService, GoogleOAuthCacheService oAuthCacheService, PasswordSettingsService passwordSettingsService, TSVPolicyProvider tsvPolicyProvider, PolicyApiCacheService policyApiCacheService, GoogleLicenseCacheService licenseCacheService, GoogleDomainCacheService domainCacheService) {
        this.usersCacheService = usersCacheService;
        this.groupsCacheService = groupsCacheService;
        this.orgUnitCacheService = orgUnitCacheService;
        this.sharedDriveCacheService = sharedDriveCacheService;
        this.mobileDeviceCacheService = mobileDeviceCacheService;
        this.appPasswordsService = appPasswordsService;
        this.adminSecurityKeysService = adminSecurityKeysService;
        this.oAuthCacheService = oAuthCacheService;
        this.passwordSettingsService = passwordSettingsService;
        this.tsvPolicyProvider = tsvPolicyProvider;
        this.policyApiCacheService = policyApiCacheService;
        this.licenseCacheService = licenseCacheService;
        this.domainCacheService = domainCacheService;
    }

    @FunctionalInterface
    private interface ThrowingRunnable {
        void run() throws Exception;
    }

    private CompletableFuture<Void> runSafeAsync(ThrowingRunnable task, String taskName) {
        return CompletableFuture.runAsync(() -> {
            try {
                task.run();
            } catch (Exception e) {
                log.warn("{} warmup failed",taskName, e);
            }
        });
    }

    public void warmupAllCachesAsync(String loggedInEmail) {
        CompletableFuture<?>[] tasks = new CompletableFuture<?>[] {
          runSafeAsync(() -> usersCacheService.forceRefreshCache(loggedInEmail), "Users"),
          runSafeAsync(() -> groupsCacheService.forceRefreshCache(loggedInEmail), "Groups"),
          runSafeAsync(() -> orgUnitCacheService.forceRefreshCache(loggedInEmail), "Org units"),
          runSafeAsync(() -> sharedDriveCacheService.forceRefreshCache(loggedInEmail), "Drives"),
          runSafeAsync(() -> mobileDeviceCacheService.forceRefreshCache(loggedInEmail), "Devices"),
          runSafeAsync(() -> appPasswordsService.forceRefreshCache(loggedInEmail), "App passwords"),
          runSafeAsync(() -> adminSecurityKeysService.forceRefreshCache(loggedInEmail), "Admin security keys"),
          runSafeAsync(() -> passwordSettingsService.getPasswordSettings(loggedInEmail), "Password settings"),
          runSafeAsync(() -> tsvPolicyProvider.forceRefreshCache(loggedInEmail), "TSV Policy"),
          runSafeAsync(() -> {
              policyApiCacheService.getAllPolicies(loggedInEmail);
              policyApiCacheService.getOuIdToPathMap(loggedInEmail);
          }, "Policy API"),
                runSafeAsync(() -> licenseCacheService.forceRefreshCache(loggedInEmail), "Licenses"),
                runSafeAsync(() -> oAuthCacheService.forceRefreshCache(loggedInEmail), "OAuth"),
                runSafeAsync(() -> domainCacheService.forceRefreshCache(loggedInEmail), "Domain"),
        };

        CompletableFuture.allOf(tasks).thenAccept(v -> log.info("✅ Cache warm-up succesvol voltooid voor alle modules voor: {}", loggedInEmail));
    }
}
