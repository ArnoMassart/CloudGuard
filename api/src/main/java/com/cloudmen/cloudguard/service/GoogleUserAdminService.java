package com.cloudmen.cloudguard.service;

import com.cloudmen.cloudguard.dto.users.UserCacheEntry;
import com.cloudmen.cloudguard.dto.users.UserOrgDetail;
import com.cloudmen.cloudguard.dto.users.UserOverviewResponse;
import com.cloudmen.cloudguard.dto.users.UserPageResponse;
import com.cloudmen.cloudguard.utility.DateTimeConverter;
import com.cloudmen.cloudguard.utility.GoogleApiFactory;
import com.cloudmen.cloudguard.utility.GoogleServiceHelperMethods;
import com.google.api.services.admin.directory.Directory;
import com.google.api.services.admin.directory.DirectoryScopes;
import com.google.api.services.admin.directory.model.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

@Service
public class GoogleUserAdminService {

    private final GoogleApiFactory directoryFactory;

    private final Map<String, UserCacheEntry> cache = new ConcurrentHashMap<>();
    private static final long CACHE_TTL_MS = 3600000L;

    private static final Logger log = LoggerFactory.getLogger(GoogleUserAdminService.class);

    public GoogleUserAdminService(GoogleApiFactory directoryFactory, UserService userService) {
        this.directoryFactory = directoryFactory;
    }

    public void forceRefreshCache(String loggedInEmail) {
        cache.compute(loggedInEmail, this::fetchFromGoogle);
    }

    public List<String> getUserRoles(String email) {
        try {
            Directory service = directoryFactory.getDirectoryService(
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

    public UserPageResponse getWorkspaceUsersPaged(String loggedInEmail, String pageToken, int size, String query) {
        // 1. Haal de lijst uit het RAM geheugen (Praat NIET met Google, tenzij de cache leeg is)
        UserCacheEntry cachedData = getOrFetchUsersData(loggedInEmail);

        // 2. Filter IN HET GEHEUGEN
        List<User> filteredList = cachedData.allUsers();
        if (query != null && !query.trim().isEmpty()) {
            String lowerQuery = query.toLowerCase().trim();
            filteredList = filteredList.stream()
                    .filter(u -> (u.getPrimaryEmail() != null && u.getPrimaryEmail().toLowerCase().contains(lowerQuery)) ||
                            (u.getName() != null && u.getName().getFullName().toLowerCase().contains(lowerQuery)))
                    .toList();
        }

        // 3. Pagineren IN HET GEHEUGEN
        int page = 1;
        if (pageToken != null && !pageToken.isEmpty()) {
            try { page = Integer.parseInt(pageToken); } catch (NumberFormatException ignored) {}
        }

        int totalUsers = filteredList.size();
        int startIndex = (page - 1) * size;
        int endIndex = Math.min(startIndex + size, totalUsers);

        List<User> pagedGoogleUsers = (startIndex >= totalUsers) ? Collections.emptyList() : filteredList.subList(startIndex, endIndex);

        // 4. Mappen naar DTO
        List<UserOrgDetail> mappedUsers = pagedGoogleUsers.stream().map(user -> {
            Long roleId = cachedData.userRoleAssignments().get(user.getPrimaryEmail());
            String roleName = (roleId != null) ? cachedData.roleDictionary().getOrDefault(roleId, "Unknown Role") : "Regular User";

            boolean isActive = !Boolean.TRUE.equals(user.getSuspended());
            boolean twoFAEnabled = Boolean.TRUE.equals(user.getIsEnrolledIn2Sv());

            return new UserOrgDetail(
                    user.getName().getFullName(),
                    user.getPrimaryEmail(),
                    GoogleServiceHelperMethods.translateRoleName(roleName),
                    isActive,
                    user.getLastLoginTime() != null ? DateTimeConverter.convertToTimeAgo(user.getLastLoginTime()) : "Nooit",
                    twoFAEnabled,
                    GoogleServiceHelperMethods.checkUserSecurityStatus(isActive, user.getLastLoginTime(), twoFAEnabled)
            );
        }).toList();

        String nextTokenToReturn = (endIndex < totalUsers) ? String.valueOf(page + 1) : null;
        return new UserPageResponse(mappedUsers, nextTokenToReturn);
    }

    public UserOverviewResponse getUsersPageOverview(String loggedInEmail) {
        UserCacheEntry cachedData = getOrFetchUsersData(loggedInEmail);
        List<User> googleUsers = cachedData.allUsers();
        LocalDate now = LocalDate.now();

        long totalUsers = googleUsers.size();
        long withoutTwoFactor = googleUsers.stream().filter(user -> !user.getSuspended() && !user.getIsEnrolledIn2Sv()).count();
        long adminUsers = googleUsers.stream().filter(User::getIsAdmin).count();
        long securityScore = calculateSecurityScore(googleUsers);

        long activeLongNoLoginCount = googleUsers.stream().filter(user -> {
            if (user.getLastLoginTime() == null) return false;
            LocalDate loginDate = DateTimeConverter.convertGoogleDateTime(user.getLastLoginTime());
            return !Boolean.TRUE.equals(user.getSuspended()) && ChronoUnit.YEARS.between(loginDate, now) >= 1;
        }).count();

        long inactiveRecentLoginCount = googleUsers.stream().filter(user -> {
            if (user.getLastLoginTime() == null) return false;
            LocalDate loginDate = DateTimeConverter.convertGoogleDateTime(user.getLastLoginTime());
            return Boolean.TRUE.equals(user.getSuspended()) && ChronoUnit.DAYS.between(loginDate, now) <= 7;
        }).count();

        return new UserOverviewResponse(totalUsers, withoutTwoFactor, adminUsers, securityScore, activeLongNoLoginCount, inactiveRecentLoginCount);
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

    private UserCacheEntry getOrFetchUsersData(String loggedInEmail) {
        // .compute() zorgt automatisch voor een 'Lock' op dit specifieke e-mailadres.
        // Als 2 calls tegelijk komen, mag er 1 naar binnen. De 2e wacht tot de 1e klaar is.
        return cache.compute(loggedInEmail, (email, existingEntry) -> {
            long now = System.currentTimeMillis();

            // Als de 1e thread net klaar is, ziet de 2e thread direct dat de data nu wél vers is!
            if (existingEntry != null && (now - existingEntry.timestamp() < CACHE_TTL_MS)) {
                return existingEntry;
            }

            // Haal nieuw op bij Google
            return fetchFromGoogle(email, existingEntry);
        });
    }

    private UserCacheEntry fetchFromGoogle(String adminEmail, UserCacheEntry fallbackEntry) {
        try {
            log.info("Ophalen LIVE data van Google voor: {}", adminEmail);
            Directory userDirectory = directoryFactory.getDirectoryService(
                    DirectoryScopes.ADMIN_DIRECTORY_USER_READONLY, adminEmail);
            Directory roleDirectory = directoryFactory.getDirectoryService(
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

    private Map<Long, String> getRoleDictionary(Directory roleDirectory) throws IOException {
        return roleDirectory.roles().list("my_customer")
                .execute().getItems().stream()
                .collect(Collectors.toMap(Role::getRoleId, Role::getRoleName, (e, r) -> e));
    }

    private int calculateSecurityScore(List<User> googleUsers) {
        if (googleUsers.isEmpty()) return 100;
        long complyCount = googleUsers.stream().filter(user -> {
            boolean isActive = !Boolean.TRUE.equals(user.getSuspended());
            boolean twoFAEnabled = Boolean.TRUE.equals(user.getIsEnrolledIn2Sv());
            return GoogleServiceHelperMethods.checkUserSecurityStatus(isActive, user.getLastLoginTime(), twoFAEnabled);
        }).count();
        return (int) Math.floor((double) complyCount / googleUsers.size() * 100);
    }
}
