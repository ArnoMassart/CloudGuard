package com.cloudmen.cloudguard.service.cache;

import com.cloudmen.cloudguard.dto.users.UserCacheEntry;
import com.cloudmen.cloudguard.exception.GoogleWorkspaceSyncException;
import com.cloudmen.cloudguard.utility.GoogleApiFactory;
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
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

@Service
public class GoogleUsersCacheService {
    private static final Logger log = LoggerFactory.getLogger(GoogleUsersCacheService.class);

    private final GoogleApiFactory googleApiFactory;

    private final Cache<String, UserCacheEntry> cache = Caffeine.newBuilder()
            .expireAfterWrite(4, TimeUnit.HOURS)
            .maximumSize(100)
            .build();

    private static final String MY_CUSTOMER_TEXT = "my_customer";

    public GoogleUsersCacheService(GoogleApiFactory googleApiFactory) {
        this.googleApiFactory = googleApiFactory;
    }

    public void forceRefreshCache(String loggedInEmail) {
        cache.asMap().compute(loggedInEmail, this::fetchFromGoogle);
    }

    public UserCacheEntry getOrFetchUsersData(String loggedInEmail) {
        return cache.get(loggedInEmail, email -> fetchFromGoogle(email, null));
    }

    public List<String> getUserRoles(String email) {
        try {
            Directory service = googleApiFactory.getDirectoryService(
                    DirectoryScopes.ADMIN_DIRECTORY_ROLEMANAGEMENT_READONLY, email);

            RoleAssignments assignments = service.roleAssignments().list(MY_CUSTOMER_TEXT)
                    .setUserKey(email)
                    .execute();
            List<RoleAssignment> items = assignments.getItems();

            if (items == null || items.isEmpty()) {
                return Collections.emptyList();
            }

            List<com.google.api.services.admin.directory.model.Role> allRoles =
                    service.roles().list(MY_CUSTOMER_TEXT).execute().getItems();

            return items.stream()
                    .map(assignment -> allRoles.stream()
                            .filter(role -> role.getRoleId().equals(assignment.getRoleId()))
                            .findFirst()
                            .map(Role::getRoleName)
                            .orElse("Unknown Role (" + assignment.getRoleId() + ")"))
                    .distinct()
                    .toList();
        } catch (Exception e) {
            throw new GoogleWorkspaceSyncException("Failed to fetch roles from Google: " + e.getMessage());
        }
    }

    private UserCacheEntry fetchFromGoogle(String adminEmail, UserCacheEntry fallbackEntry) {
        try {
            log.info("Ophalen LIVE Gebruiker data van Google voor: {}", adminEmail);
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
