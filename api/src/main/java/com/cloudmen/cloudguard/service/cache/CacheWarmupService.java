package com.cloudmen.cloudguard.service.cache;

import com.cloudmen.cloudguard.domain.model.User;
import com.cloudmen.cloudguard.domain.model.UserRole;
import com.cloudmen.cloudguard.service.*;
import com.cloudmen.cloudguard.service.policy.MobileManagementPolicyProvider;
import com.cloudmen.cloudguard.service.policy.TSVPolicyProvider;
import com.cloudmen.cloudguard.utility.GoogleServiceHelperMethods;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
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
    private final MobileManagementPolicyProvider mobileManagementPolicyProvider;
    private final PolicyApiCacheService policyApiCacheService;

    private final GoogleLicenseCacheService licenseCacheService;
    private final GoogleDomainCacheService domainCacheService;
    private final UserService userService;
    private final OrganizationService organizationService;

    public CacheWarmupService(GoogleUsersCacheService usersCacheService, GoogleGroupsCacheService groupsCacheService, GoogleOrgUnitCacheService orgUnitCacheService, GoogleSharedDriveCacheService sharedDriveCacheService, GoogleDeviceCacheService mobileDeviceCacheService, AppPasswordsService appPasswordsService, AdminSecurityKeysService adminSecurityKeysService, GoogleOAuthCacheService oAuthCacheService, PasswordSettingsService passwordSettingsService, TSVPolicyProvider tsvPolicyProvider, MobileManagementPolicyProvider mobileManagementPolicyProvider, PolicyApiCacheService policyApiCacheService, GoogleLicenseCacheService licenseCacheService, GoogleDomainCacheService domainCacheService, UserService userService, OrganizationService organizationService) {
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
        this.mobileManagementPolicyProvider = mobileManagementPolicyProvider;
        this.policyApiCacheService = policyApiCacheService;
        this.licenseCacheService = licenseCacheService;
        this.domainCacheService = domainCacheService;
        this.userService = userService;
        this.organizationService = organizationService;
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

    public CompletableFuture<Void> warmupAllCachesAsync(String loggedInEmail) {
        Optional<User> user = userService.findByEmail(loggedInEmail);
        String adminEmail = GoogleServiceHelperMethods.getAdminEmailForUser(loggedInEmail, userService, organizationService);

        if (user.isEmpty()) return CompletableFuture.completedFuture(null);

        List<UserRole> roles = user.get().getRoles();

        if (roles.isEmpty() || roles.contains(UserRole.UNASSIGNED)) {
            log.info("⏭️ Geen cache warm-up nodig (gebruiker heeft geen rechten): {}", loggedInEmail);
            return CompletableFuture.completedFuture(null);
        }

        boolean isSuperAdmin = roles.contains(UserRole.SUPER_ADMIN);

        List<CompletableFuture<?>> tasks = new ArrayList<>();

        if (isSuperAdmin || roles.contains(UserRole.USERS_GROUPS_VIEWER)) {
            tasks.add(runSafeAsync(() -> usersCacheService.forceRefreshCache(loggedInEmail), "Users"));
            tasks.add(runSafeAsync(() -> groupsCacheService.forceRefreshCache(loggedInEmail), "Groups"));
        }

        if (isSuperAdmin || roles.contains(UserRole.ORG_UNITS_VIEWER)) {
            tasks.add(runSafeAsync(() -> orgUnitCacheService.forceRefreshCache(loggedInEmail), "Org units"));
            tasks.add(runSafeAsync(() -> tsvPolicyProvider.forceRefreshCache(adminEmail), "TSV Policy"));
            tasks.add(runSafeAsync(() -> mobileManagementPolicyProvider.forceRefreshCache(adminEmail), "Mobile Management Policy"));
            tasks.add(runSafeAsync(() -> {
                policyApiCacheService.getAllPolicies(adminEmail);
                policyApiCacheService.getOuIdToPathMap(adminEmail);
            }, "Policy API"));
        }

        if (isSuperAdmin || roles.contains(UserRole.SHARED_DRIVES_VIEWER)) {
            tasks.add(runSafeAsync(() -> sharedDriveCacheService.forceRefreshCache(loggedInEmail), "Drives"));
        }

        if (isSuperAdmin || roles.contains(UserRole.DEVICES_VIEWER)){
            tasks.add(runSafeAsync(() -> mobileDeviceCacheService.forceRefreshCache(loggedInEmail), "Devices"));
        }

        if (isSuperAdmin || roles.contains(UserRole.APP_ACCESS_VIEWER)) {
            tasks.add(runSafeAsync(() -> oAuthCacheService.forceRefreshCache(loggedInEmail), "OAuth"));
        }

        if (isSuperAdmin || roles.contains(UserRole.APP_PASSWORDS_VIEWER)) {
            tasks.add(runSafeAsync(() -> appPasswordsService.forceRefreshCache(loggedInEmail), "App passwords"));
        }

        if (isSuperAdmin || roles.contains(UserRole.PASSWORD_SETTINGS_VIEWER)) {
            tasks.add(runSafeAsync(() -> passwordSettingsService.getPasswordSettings(loggedInEmail), "Password settings"));
            tasks.add(runSafeAsync(() -> adminSecurityKeysService.forceRefreshCache(loggedInEmail), "Admin security keys"));
        }

        if (isSuperAdmin || roles.contains(UserRole.DOMAIN_DNS_VIEWER)) {
            tasks.add(runSafeAsync(() -> domainCacheService.forceRefreshCache(loggedInEmail), "Domain"));
        }

        if (isSuperAdmin || roles.contains(UserRole.LICENSES_VIEWER)) {
            tasks.add(runSafeAsync(() -> licenseCacheService.forceRefreshCache(loggedInEmail), "Licenses"));
        }

        if (tasks.isEmpty()) {
            log.info("⏭️ Geen cache warm-up nodig (gebruiker heeft geen rechten): {}", loggedInEmail);
            return CompletableFuture.completedFuture(null);
        }

        CompletableFuture<?>[] tasksArray = tasks.toArray(new CompletableFuture[0]);

        return CompletableFuture.allOf(tasksArray).thenAccept(v -> log.info("✅ Cache warm-up succesvol voltooid voor alle {} modules voor: {}", tasks.size() ,loggedInEmail));
    }
}
