package com.cloudmen.cloudguard.service.cache;

import com.cloudmen.cloudguard.dto.users.UserCacheEntry;
import com.cloudmen.cloudguard.service.GoogleUsersService;
import com.cloudmen.cloudguard.utility.GoogleApiFactory;
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
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

@Service
public class GoogleUsersCacheService {
    private static final Logger log = LoggerFactory.getLogger(GoogleUsersCacheService.class);

    private final GoogleApiFactory googleApiFactory;

    private final Map<String, UserCacheEntry> cache = new ConcurrentHashMap<>();
    private static final long CACHE_TTL_MS = 3600000L;

    public GoogleUsersCacheService(GoogleApiFactory googleApiFactory) {
        this.googleApiFactory = googleApiFactory;
    }

    public void forceRefreshCache(String loggedInEmail) {
        cache.compute(loggedInEmail, this::fetchFromGoogle);
    }

    public UserCacheEntry getOrFetchUsersData(String loggedInEmail) {
        // .compute() zorgt automatisch voor een 'Lock' op dit specifieke e-mailadres.
        // Als 2 calls tegelijk komen, mag er 1 naar binnen. De 2e wacht tot de 1e klaar is.
        return cache.compute(loggedInEmail, (email, entry) -> {
            long now = System.currentTimeMillis();

            // Als de 1e thread net klaar is, ziet de 2e thread direct dat de data nu wél vers is!
            if (entry != null && (now - entry.timestamp() < CACHE_TTL_MS)) {
                return entry;
            }

            // Haal nieuw op bij Google
            return fetchFromGoogle(email, entry);
        });
    }

    public List<String> getUserRoles(String email) {
        try {
            Directory service = googleApiFactory.getDirectoryService(
                    DirectoryScopes.ADMIN_DIRECTORY_ROLEMANAGEMENT_READONLY, email);

            RoleAssignments assignments = service.roleAssignments().list("my_customer")
                    .setUserKey(email)
                    .execute();
            List<RoleAssignment> items = assignments.getItems();

            if (items == null || items.isEmpty()) {
                return Collections.emptyList();
            }

            List<com.google.api.services.admin.directory.model.Role> allRoles =
                    service.roles().list("my_customer").execute().getItems();

            return items.stream()
                    .map(assignment -> allRoles.stream()
                            .filter(role -> role.getRoleId().equals(assignment.getRoleId()))
                            .findFirst()
                            .map(Role::getRoleName)
                            .orElse("Unknown Role (" + assignment.getRoleId() + ")"))
                    .distinct()
                    .collect(Collectors.toList());
        } catch (Exception e) {
            throw new RuntimeException("Failed to fetch roles from Google: " + e.getMessage());
        }
    }

    private UserCacheEntry fetchFromGoogle(String adminEmail, UserCacheEntry fallbackEntry) {
        try {
            log.info("Ophalen LIVE Gebruiker data van Google voor: {}", adminEmail);
            Directory userDirectory = googleApiFactory.getDirectoryService(
                    DirectoryScopes.ADMIN_DIRECTORY_USER_READONLY, adminEmail);
            Directory roleDirectory = googleApiFactory.getDirectoryService(
                    DirectoryScopes.ADMIN_DIRECTORY_ROLEMANAGEMENT_READONLY, adminEmail);

            // A. Haal alle gebruikers op
            List<User> allUsers = fetchAllOrgUsers(userDirectory);

            // B. Haal rollen woordenboek op
            Map<Long, String> roleDictionary = getRoleDictionary(roleDirectory);

            // C. Haal ALLE role-assignments op
            Map<String, Long> userRoleAssignments = roleDirectory.roleAssignments().list("my_customer")
                    .execute().getItems().stream()
                    .collect(Collectors.toMap(RoleAssignment::getAssignedTo, RoleAssignment::getRoleId, (e, r) -> e));

            return new UserCacheEntry(
                    allUsers, roleDictionary, userRoleAssignments, System.currentTimeMillis());

        } catch (Exception e) {
            if (fallbackEntry != null) {
                log.error("Google API faalde! Terugvallen op oude cache: {}", e.getMessage());
                return fallbackEntry;
            }
            throw new RuntimeException("Fout bij ophalen Google data, en geen cache beschikbaar: " + e.getMessage());
        }
    }

    private List<User> fetchAllOrgUsers(Directory service) throws IOException {
        List<User> googleUsers = new ArrayList<>();
        String pageToken = null;

        do {
            Users result = service.users().list()
                    .setCustomer("my_customer")
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
        return roleDirectory.roles().list("my_customer")
                .execute().getItems().stream()
                .collect(Collectors.toMap(Role::getRoleId, Role::getRoleName, (e, r) -> e));
    }
}
