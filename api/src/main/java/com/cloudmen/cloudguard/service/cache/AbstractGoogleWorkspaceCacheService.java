package com.cloudmen.cloudguard.service.cache;

import com.cloudmen.cloudguard.service.OrganizationService;
import com.cloudmen.cloudguard.service.user.UserService;
import com.cloudmen.cloudguard.utility.GoogleServiceHelperMethods;
import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;

import java.util.concurrent.TimeUnit;

/**
 * Abstract base service providing common caching logic, tenant (admin) resolution,
 * and lifecycle management for all Google Workspace cache services.
 *
 * @param <T> the type of the DTO/Entry stored in the cache (e.g., UserCacheEntry, DeviceCacheEntry)
 */
public abstract class AbstractGoogleWorkspaceCacheService<T> {

    private final UserService userService;
    private final OrganizationService organizationService;
    private final Cache<String, T> cache;

    protected AbstractGoogleWorkspaceCacheService(UserService userService, OrganizationService organizationService, long expireDuration, TimeUnit timeUnit) {
        this.userService = userService;
        this.organizationService = organizationService;
        this.cache = Caffeine.newBuilder()
                .expireAfterWrite(expireDuration, timeUnit)
                .maximumSize(100)
                .build();
    }

    protected AbstractGoogleWorkspaceCacheService(UserService userService, OrganizationService organizationService, long expireDurationHours) {
        this(userService, organizationService, expireDurationHours, TimeUnit.HOURS);
    }

    public void forceRefreshCache(String loggedInEmail) {
        String adminEmail = GoogleServiceHelperMethods.getAdminEmailForUser(loggedInEmail, userService, organizationService);
        cache.asMap().compute(adminEmail, this::fetchFromGoogle);
    }

    public T getOrFetchData(String loggedInEmail) {
        String adminEmail = GoogleServiceHelperMethods.getAdminEmailForUser(loggedInEmail, userService, organizationService);
        return cache.get(adminEmail, key -> fetchFromGoogle(key, null));
    }

    public T getOrFetchDataByAdmin(String adminEmail) {
        return cache.get(adminEmail, key -> fetchFromGoogle(key, null));
    }

    protected abstract T fetchFromGoogle(String adminEmail, T fallBackEntry);
}
