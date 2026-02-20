package com.cloudmen.cloudguard.service;

import com.cloudmen.cloudguard.dto.UserOrgDetail;
import com.cloudmen.cloudguard.dto.UserOverviewResponse;
import com.cloudmen.cloudguard.dto.UserPageResponse;
import com.cloudmen.cloudguard.utility.DateTimeConverter;
import com.cloudmen.cloudguard.utility.GoogleServiceHelperMethods;
import com.google.api.services.admin.directory.Directory;
import com.google.api.services.admin.directory.DirectoryScopes;
import com.google.api.services.admin.directory.model.*;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
public class GoogleUserAdminService {

    private final GoogleApiFactory directoryFactory;

    public GoogleUserAdminService(GoogleApiFactory directoryFactory) {
        this.directoryFactory = directoryFactory;
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
        try {
            Directory userDirectory = directoryFactory.getDirectoryService(
                    DirectoryScopes.ADMIN_DIRECTORY_USER_READONLY, loggedInEmail);
            Directory roleDirectory = directoryFactory.getDirectoryService(
                    DirectoryScopes.ADMIN_DIRECTORY_ROLEMANAGEMENT_READONLY, loggedInEmail);

            Map<Long, String> roleDictionary = roleDirectory.roles().list("my_customer")
                    .execute()
                    .getItems()
                    .stream()
                    .collect(Collectors.toMap(
                            Role::getRoleId,
                            Role::getRoleName,
                            (existing, replacement) -> existing
                    ));

            Directory.Users.List request = userDirectory.users().list()
                    .setCustomer("my_customer")
                    .setProjection("full")
                    .setMaxResults(size)
                    .setPageToken(pageToken);

            if (query != null && !query.trim().isEmpty()) {
                String cleanQuery = query.trim();

                if (cleanQuery.contains("@")) {
                    request.setQuery("email:" + cleanQuery + "*");
                } else {
                    request.setQuery("givenName:" + cleanQuery + "*");
                }
            } else {
                request.setOrderBy("given_name");
            }

            Users result = request.execute();
            List<User> googleUsers = result.getUsers();

            if (googleUsers == null) {
                return new UserPageResponse(Collections.emptyList(), null);
            }

            List<UserOrgDetail> mappedUsers = googleUsers.stream().map(user -> {
                String firstRole = getFirstRoleForUser(roleDirectory, user.getPrimaryEmail(), roleDictionary);

                boolean isActive = !Boolean.TRUE.equals(user.getSuspended());
                boolean twoFAEnabled = Boolean.TRUE.equals(user.getIsEnrolledIn2Sv());

                return new UserOrgDetail(
                        user.getName().getFullName(),
                        user.getPrimaryEmail(),
                        GoogleServiceHelperMethods.translateRoleName(firstRole),
                        isActive,
                        user.getLastLoginTime() != null ? DateTimeConverter.convertToTimeAgo(user.getLastLoginTime()) : "Nooit",
                        twoFAEnabled,
                        GoogleServiceHelperMethods.checkUserSecurityStatus(isActive, user.getLastLoginTime(), twoFAEnabled)
                );
            }).toList();

            return new UserPageResponse(mappedUsers, result.getNextPageToken());
        } catch (Exception e) {
            throw new RuntimeException("Failed to fetch users from Google: " + e.getMessage());
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

    private String getFirstRoleForUser(Directory service, String email, Map<Long, String> roleDictionary) {
        try {
            RoleAssignments assignments = service.roleAssignments()
                    .list("my_customer")
                    .setUserKey(email)
                    .execute();

            if (assignments.getItems() == null || assignments.getItems().isEmpty()) {
                return "Regular User";
            }

            Long firstRoleId = assignments.getItems().get(0).getRoleId();
            String name = roleDictionary.getOrDefault(firstRoleId, "Unknown Role");

            return GoogleServiceHelperMethods.translateRoleName(name);
        } catch (IOException e) {
            return "Error fetching role";
        }
    }

    public UserOverviewResponse getUsersPageOverview(String loggedInEmail) {
        try {
            LocalDate now = LocalDate.now();

            Directory userDirectory = directoryFactory.getDirectoryService(
                    DirectoryScopes.ADMIN_DIRECTORY_USER_READONLY, loggedInEmail);

            List<User> googleUsers = fetchAllOrgUsers(userDirectory);

            long totalUsers = googleUsers.size();
            long withoutTwoFactor = googleUsers.stream()
                    .filter(user -> !user.getSuspended() && !user.getIsEnrolledIn2Sv())
                    .count();
            long adminUsers = googleUsers.stream().filter(User::getIsAdmin).count();
            long securityScore = calculateSecurityScore(googleUsers);

            long activeLongNoLoginCount = googleUsers.stream().filter(user -> {
                LocalDate loginDate = DateTimeConverter.convertGoogleDateTime(user.getLastLoginTime());
                boolean isActive = !Boolean.TRUE.equals(user.getSuspended());
                long yearsSinceLogin = ChronoUnit.YEARS.between(loginDate, now);
                return isActive && yearsSinceLogin >= 1;
            }).count();

            long inactiveRecentLoginCount = googleUsers.stream().filter(user -> {
                LocalDate loginDate = DateTimeConverter.convertGoogleDateTime(user.getLastLoginTime());
                boolean isActive = !Boolean.TRUE.equals(user.getSuspended());
                long daysSinceLogin = ChronoUnit.DAYS.between(loginDate, now);
                return !isActive && daysSinceLogin <= 7;
            }).count();

            return new UserOverviewResponse(
                    totalUsers,
                    withoutTwoFactor,
                    adminUsers,
                    securityScore,
                    activeLongNoLoginCount,
                    inactiveRecentLoginCount
            );
        } catch (Exception e) {
            throw new RuntimeException("Failed to fetch users from Google: " + e.getMessage());
        }
    }

    private int calculateSecurityScore(List<User> googleUsers) {
        int totalUsers = googleUsers.size();
        long complyCount = googleUsers.stream().filter(user -> {
            boolean isActive = !Boolean.TRUE.equals(user.getSuspended());
            boolean twoFAEnabled = Boolean.TRUE.equals(user.getIsEnrolledIn2Sv());
            return GoogleServiceHelperMethods.checkUserSecurityStatus(isActive, user.getLastLoginTime(), twoFAEnabled);
        }).count();
        return (int) Math.floor((double) complyCount / totalUsers * 100);
    }
}
