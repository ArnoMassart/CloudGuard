package com.cloudmen.cloudguard.service;

import com.cloudmen.cloudguard.dto.GroupOrgDetail;
import com.cloudmen.cloudguard.dto.UserOrgDetail;
import com.cloudmen.cloudguard.dto.UserOverviewResponse;
import com.cloudmen.cloudguard.dto.UserPageResponse;
import com.cloudmen.cloudguard.utility.DateTimeConverter;
import com.cloudmen.cloudguard.utility.GoogleServiceHelperMethods;
import com.google.api.client.googleapis.javanet.GoogleNetHttpTransport;
import com.google.api.client.json.gson.GsonFactory;
import com.google.api.services.admin.directory.Directory;
import com.google.api.services.admin.directory.DirectoryScopes;
import com.google.api.services.admin.directory.model.*;
import com.google.api.services.groupssettings.Groupssettings;
import com.google.api.services.groupssettings.GroupssettingsScopes;
import com.google.api.services.groupssettings.model.Groups;
import com.google.auth.http.HttpCredentialsAdapter;
import com.google.auth.oauth2.ServiceAccountCredentials;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import java.util.Set;

@Service
public class GoogleAdminService {

    private static final Logger log = LoggerFactory.getLogger(GoogleAdminService.class);

    @Value("${google.api.client-email}")
    private String clientEmail;

    @Value("${google.api.private-key}")
    private String privateKey;

    private static final String GROUPS_SETTINGS_SCOPE = "https://www.googleapis.com/auth/apps.groups.settings";

    private Directory getDirectoryService(String scope, String loggedInEmail) throws Exception {
        return getDirectoryService(Collections.singleton(scope), loggedInEmail);
    }

    private Directory getDirectoryService(Set<String> scopes, String loggedInEmail) throws Exception {
        String pk = privateKey.replace("\\n", "\n");

        ServiceAccountCredentials credentials = ServiceAccountCredentials.newBuilder()
                .setClientEmail(clientEmail)
                .setPrivateKey(GoogleServiceHelperMethods.decodePrivateKey(pk))
                .setServiceAccountUser(loggedInEmail)
                .setScopes(scopes)
                .build();

        return new Directory.Builder(
                GoogleNetHttpTransport.newTrustedTransport(),
                GsonFactory.getDefaultInstance(),
                new HttpCredentialsAdapter(credentials))
                .setApplicationName("CloudGuard")
                .build();
    }

    public List<String> getUserRoles(String email) {
        try {
            Directory service = getDirectoryService(DirectoryScopes.ADMIN_DIRECTORY_ROLEMANAGEMENT_READONLY, email);

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
            Directory userDirectory = getDirectoryService(DirectoryScopes.ADMIN_DIRECTORY_USER_READONLY, loggedInEmail);
            Directory roleDirectory = getDirectoryService(DirectoryScopes.ADMIN_DIRECTORY_ROLEMANAGEMENT_READONLY, loggedInEmail);

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

                // Slim zoeken: als er een '@' in zit is het een email, anders een naam
                if (cleanQuery.contains("@")) {
                    request.setQuery("email:" + cleanQuery + "*");
                } else {
                    request.setQuery("givenName:" + cleanQuery + "*");
                }
            } else {
                // Alleen sorteren op naam als we de standaard lijst ophalen
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
                        GoogleServiceHelperMethods.checkSecurityStatus(isActive, user.getLastLoginTime(), twoFAEnabled)
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
            // Haal de rol-koppelingen op voor deze specifieke gebruiker
            RoleAssignments assignments = service.roleAssignments()
                    .list("my_customer")
                    .setUserKey(email)
                    .execute();

            if (assignments.getItems() == null || assignments.getItems().isEmpty()) {
                return "Regular User";
            }

            // Pak de RoleId van de eerste assignment
            Long firstRoleId = assignments.getItems().get(0).getRoleId();

            // Zoek de naam op in ons woordenboek (geen API call nodig!)
            String name = roleDictionary.getOrDefault(firstRoleId, "Unknown Role");

            // Optioneel: vertaal systeemrollen naar mooie namen
            return GoogleServiceHelperMethods.translateRoleName(name);

        } catch (IOException e) {
            return "Error fetching role";
        }
    }

    public UserOverviewResponse getUsersPageOverview(String loggedInEmail) {

        try {
            LocalDate now = LocalDate.now();

            Directory userDirectory = getDirectoryService(DirectoryScopes.ADMIN_DIRECTORY_USER_READONLY, loggedInEmail);

            List<User> googleUsers = fetchAllOrgUsers(userDirectory);

            long totalUsers = googleUsers.size();

            long withoutTwoFactor = googleUsers.stream().filter(user -> !user.getSuspended() && !user.getIsEnrolledIn2Sv()).count();

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

        // Total score is calculate the conformity of each user and check if they comply
        long complyCount = googleUsers.stream().filter(user -> {
            boolean isActive = !Boolean.TRUE.equals(user.getSuspended());
            boolean twoFAEnabled = Boolean.TRUE.equals(user.getIsEnrolledIn2Sv());
            return GoogleServiceHelperMethods.checkSecurityStatus(isActive, user.getLastLoginTime(), twoFAEnabled);
        }).count();


        return (int) Math.floor((double) complyCount / totalUsers * 100);
    }

    public List<GroupOrgDetail> getAllWorkspaceGroups(String loggedInEmail) {
        try {
            Set<String> scopes = Set.of(
                DirectoryScopes.ADMIN_DIRECTORY_GROUP_READONLY,
                DirectoryScopes.ADMIN_DIRECTORY_GROUP_MEMBER_READONLY
            );
            Directory service = getDirectoryService(scopes, loggedInEmail);

            Groupssettings settingsService = null;
            try {
                settingsService = getGroupsSettingsService(loggedInEmail);
            } catch (Throwable t) {
                log.warn("Groups Settings API unavailable", t);            
            }

            String primaryDomain = loggedInEmail.substring(loggedInEmail.indexOf('@') + 1);
            List<GroupOrgDetail> result = new ArrayList<>();
            String pageToken = null;

            do {
                com.google.api.services.admin.directory.model.Groups groupsResult =
                        service.groups().list()
                                .setCustomer("my_customer")
                                .setMaxResults(200)
                                .setPageToken(pageToken)
                                .setOrderBy("email")
                                .execute();

                List<com.google.api.services.admin.directory.model.Group> googleGroups =
                        groupsResult.getGroups();

                if (googleGroups != null) {
                    for (com.google.api.services.admin.directory.model.Group group : googleGroups) {
                        try {
                            String groupEmail = group.getEmail();
                            if (groupEmail == null || groupEmail.isBlank()) continue;

                            int total = 0;
                            int external = 0;

                            try {
                                String memberPageToken = null;
                                do {
                                    Members members = service.members().list(groupEmail)
                                            .setMaxResults(200)
                                            .setPageToken(memberPageToken)
                                            .execute();

                                    if (members.getMembers() != null) {
                                        for (Member member : members.getMembers()) {
                                            if ("USER".equals(member.getType())) {
                                                total++;
                                                String email = member.getEmail();
                                                if (email != null && !email.endsWith("@" + primaryDomain)) {
                                                    external++;
                                                }
                                            }
                                        }
                                    }
                                    memberPageToken = members.getNextPageToken();
                                } while (memberPageToken != null);
                            } catch (Exception e) {
                                // Members listing may fail for restricted groups
                            }

                            boolean externalAllowed = external > 0;
                            String whoCanJoin = "—";
                            String whoCanView = "—";

                            if (settingsService != null) {
                                try {
                                    Groups settings = settingsService.groups().get(groupEmail).execute();
                                    whoCanJoin = mapWhoCanJoin(settings.getWhoCanJoin());
                                    whoCanView = mapWhoCanViewMembership(settings.getWhoCanViewMembership());
                                } catch (Throwable t) {
                                    log.warn("Could not fetch Groups Settings for {}: {}", groupEmail, t.getMessage());
                                }
                            }

                            String risk = deriveRisk(external, total, externalAllowed);
                            List<String> tags = deriveTags(risk, externalAllowed, groupEmail);

                            result.add(new GroupOrgDetail(
                                    groupEmail,
                                    risk,
                                    tags,
                                    total,
                                    external,
                                    externalAllowed,
                                    whoCanJoin,
                                    whoCanView
                            ));
                        } catch (Throwable t) {
                            // Skip this group but continue with others
                        }
                    }
                }
                pageToken = groupsResult.getNextPageToken();
            } while (pageToken != null);

            return result;
        } catch (Exception e) {
            throw new RuntimeException("Failed to fetch groups from Google: " + e.getMessage());
        }
    }

    private Groupssettings getGroupsSettingsService(String loggedInEmail) throws Exception {
        String pk = privateKey.replace("\\n", "\n");

        ServiceAccountCredentials credentials = ServiceAccountCredentials.newBuilder()
        .setClientEmail(clientEmail)
        .setPrivateKey(decodePrivateKey(pk))
        .setServiceAccountUser(loggedInEmail)
        .setScopes(Collections.singleton(GROUPS_SETTINGS_SCOPE))
        .build();

        return new Groupssettings.Builder(
            GoogleNetHttpTransport.newTrustedTransport(),
            GsonFactory.getDefaultInstance(),
            new HttpCredentialsAdapter(credentials))
            .setApplicationName("CloudGuard")
            .build();
    }

    private String deriveRisk(int external, int total, boolean externalAllowed) {
        if(external>0) return "HIGH";
        if(external<0) return "MEDIUM";
        return "LOW";
    }

    private List<String> deriveTags(String risk, boolean hasExternal, String groupEmail) {
        List<String> tags = new   ArrayList<>();
        tags.add(switch(risk){
            case "HIGH" -> "Hoog Risico";
            case "MEDIUM" -> "Middel Risico";
            default -> "Laag Risico";
        });
        if(hasExternal){
            tags.add("Extern");
        }
        if(groupEmail!= null && groupEmail.toLowerCase().contains("mailing")){
            tags.add("Mailing");
        }
        return tags;
    }

    private String mapWhoCanJoin(String who) {
        if (who == null || who.isBlank()) return "—";
        return switch (who) {
            case "ANYONE_CAN_JOIN" -> "Iedereen kan lid worden";
            case "INVITED_CAN_JOIN" -> "Alleen uitgenodigde gebruikers";
            case "CAN_REQUEST_TO_JOIN" -> "Kan verzoek doen om lid te worden";
            case "ALL_IN_DOMAIN_CAN_JOIN" -> "Iedereen in het domein";
            default -> who;
        };
    }

    private String mapWhoCanViewMembership(String who) {
        if (who == null || who.isBlank()) return "—";
        return switch (who) {
            case "ALL_IN_DOMAIN_CAN_VIEW" -> "Iedereen in het domein";
            case "ALL_MANAGERS_CAN_VIEW" -> "Alle beheerders";
            case "ALL_MEMBERS_CAN_VIEW" -> "Alle leden";
            default -> who;
        };
    }
}
