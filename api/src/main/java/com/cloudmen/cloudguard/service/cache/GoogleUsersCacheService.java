package com.cloudmen.cloudguard.service.cache;

import com.cloudmen.cloudguard.dto.users.UserCacheEntry;
import com.cloudmen.cloudguard.exception.GoogleWorkspaceSyncException;
import com.cloudmen.cloudguard.service.OrganizationService;
import com.cloudmen.cloudguard.service.UserService;
import com.cloudmen.cloudguard.utility.GoogleApiFactory;
import com.cloudmen.cloudguard.utility.GoogleServiceHelperMethods;
import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import com.google.api.services.admin.directory.Directory;
import com.google.api.services.admin.directory.DirectoryScopes;
import com.google.api.services.admin.directory.model.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

/**
 * A service responsible for retrieving and caching Google Workspace user accounts and their assigned admin roles. <p>
 *
 * This service is critical for both data visualization and access control (RBAC). It fetches all users in the domain
 * and maps their identifiers to their assigned Google Workspace administrative roles. By caching this heavy API
 * response, the application significantly reduces latency during login and dashboard rendering.
 */
@Service
public class GoogleUsersCacheService {
    private static final Logger log = LoggerFactory.getLogger(GoogleUsersCacheService.class);
    private static final String MY_CUSTOMER_TEXT = "my_customer";

    private final GoogleApiFactory googleApiFactory;
    private final UserService userService;
    private final OrganizationService organizationService;

    private final Cache<String, UserCacheEntry> cache = Caffeine.newBuilder()
            .expireAfterWrite(4, TimeUnit.HOURS)
            .maximumSize(100)
            .build();

    public GoogleUsersCacheService(GoogleApiFactory googleApiFactory, UserService userService, OrganizationService organizationService) {
        this.googleApiFactory = googleApiFactory;
        this.userService = userService;
        this.organizationService = organizationService;
    }

    /**
     * Forces a background refresh of the user data cache for the specified admin, bypassing the current
     * Time-To-Live (TTL).
     *
     * @param loggedInEmail the email of the authenticated user triggering the manual refresh
     */
    public void forceRefreshCache(String loggedInEmail) {
        cache.asMap().compute(loggedInEmail, this::fetchFromGoogle);
    }

    /**
     * Retrieves the aggregated user and role data from the cache, or synchronously fetches it from the Google Admin
     * Directory API if the cache is empty or expired.
     *
     * @param loggedInEmail the email of the authenticated user
     * @return the {@link UserCacheEntry} containing the user list and their associated role mappings
     */
    public UserCacheEntry getOrFetchUsersData(String loggedInEmail) {
        return cache.get(loggedInEmail, email -> fetchFromGoogle(email, null));
    }

    private UserCacheEntry fetchFromGoogle(String loggedInEmail, UserCacheEntry fallbackEntry) {
        try {
            String adminEmail = GoogleServiceHelperMethods.getAdminEmailForUser(loggedInEmail, userService, organizationService);

            log.info("Ophalen LIVE gebruikers data van Google. Gebruiker: {}, Impersonatie via Admin: {}", loggedInEmail, adminEmail);
            Directory userDirectory = googleApiFactory.getDirectoryService(
                    Set.of(DirectoryScopes.ADMIN_DIRECTORY_USER_READONLY, DirectoryScopes.ADMIN_DIRECTORY_USER_SECURITY),
                    adminEmail);
            Directory roleDirectory = googleApiFactory.getDirectoryService(
                    DirectoryScopes.ADMIN_DIRECTORY_ROLEMANAGEMENT_READONLY, adminEmail);

            // A. Haal alle gebruikers op
            List<User> allUsers = fetchAllOrgUsers(userDirectory);

            // B. Haal rollen woordenboek op
            Map<Long, String> roleDictionary = getRoleDictionary(roleDirectory);

            // C. Haal ALLE role-assignments op
            Map<String, Long> userRoleAssignments = roleDirectory.roleAssignments().list(MY_CUSTOMER_TEXT)
                    .execute().getItems().stream()
                    .collect(Collectors.toMap(RoleAssignment::getAssignedTo, RoleAssignment::getRoleId, (e, r) -> e));

            return new UserCacheEntry(
                    allUsers, roleDictionary, userRoleAssignments, System.currentTimeMillis());

        } catch (Exception e) {
            if (fallbackEntry != null) {
                log.error("Google API faalde! Terugvallen op oude cache: {}", e.getMessage());
                return fallbackEntry;
            }
            throw new GoogleWorkspaceSyncException("Fout bij ophalen Google gebruikers, en geen cache beschikbaar: " + e.getMessage());
        }
    }

    private List<User> fetchAllOrgUsers(Directory service) throws IOException {
        List<User> googleUsers = new ArrayList<>();
        String pageToken = null;

        do {
            Users result = service.users().list()
                    .setCustomer(MY_CUSTOMER_TEXT)
                    .setProjection("full")
                    .setMaxResults(100)
                    .setPageToken(pageToken)
                    .execute();

            if (result.getUsers() != null) {
                googleUsers.addAll(result.getUsers());
            }

            pageToken = result.getNextPageToken();
        } while (pageToken != null);

        return googleUsers;
    }

    private Map<Long, String> getRoleDictionary(Directory roleDirectory) throws IOException {
        return roleDirectory.roles().list(MY_CUSTOMER_TEXT)
                .execute().getItems().stream()
                .collect(Collectors.toMap(Role::getRoleId, Role::getRoleName, (e, r) -> e));
    }
}
