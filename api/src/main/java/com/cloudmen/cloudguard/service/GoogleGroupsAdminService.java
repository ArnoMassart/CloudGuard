package com.cloudmen.cloudguard.service;

import com.cloudmen.cloudguard.dto.GroupOrgDetail;
import com.cloudmen.cloudguard.dto.GroupOverviewResponse;
import com.cloudmen.cloudguard.dto.GroupPageResponse;
import com.cloudmen.cloudguard.dto.GroupSettingsDto;
import com.cloudmen.cloudguard.utility.GoogleApiFactory;
import com.google.api.client.googleapis.javanet.GoogleNetHttpTransport;
import com.google.api.client.json.gson.GsonFactory;
import com.google.api.services.admin.directory.Directory;
import com.google.api.services.admin.directory.DirectoryScopes;
import com.google.api.services.admin.directory.model.*;
import com.google.api.services.groupssettings.Groupssettings;
import com.google.api.services.groupssettings.model.Groups;
import com.google.api.services.cloudidentity.v1.CloudIdentity;
import com.google.api.services.cloudidentity.v1.model.LookupGroupNameResponse;
import com.google.auth.http.HttpCredentialsAdapter;
import com.google.auth.oauth2.ServiceAccountCredentials;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Set;

import static com.cloudmen.cloudguard.utility.GoogleServiceHelperMethods.decodePrivateKey;

@Service
public class GoogleGroupsAdminService {

    private static final Logger log = LoggerFactory.getLogger(GoogleGroupsAdminService.class);

    private static final String GROUPS_SETTINGS_SCOPE = "https://www.googleapis.com/auth/apps.groups.settings";
    private static final String CLOUD_IDENTITY_SCOPE = "https://www.googleapis.com/auth/cloud-identity.groups.readonly";

    private final GoogleApiFactory directoryFactory;

    @Value("${google.api.client-email}")
    private String clientEmail;

    @Value("${google.api.private-key}")
    private String privateKey;

    public GoogleGroupsAdminService(GoogleApiFactory directoryFactory) {
        this.directoryFactory = directoryFactory;
    }

    public GroupPageResponse getGroupsPaged(String loggedInEmail, String query, String pageToken, int size) {
        try {
            Set<String> scopes = Set.of(
                    DirectoryScopes.ADMIN_DIRECTORY_GROUP_READONLY,
                    DirectoryScopes.ADMIN_DIRECTORY_GROUP_MEMBER_READONLY
            );
            Directory service = directoryFactory.getDirectoryService(scopes, loggedInEmail);

            CloudIdentity cloudIdentity = null;
            try {
                cloudIdentity = getCloudIdentityService(loggedInEmail);
            } catch (Throwable t) {
                log.warn("Cloud Identity API unavailable", t);
            }

            String primaryDomain = loggedInEmail.substring(loggedInEmail.indexOf('@') + 1);
            List<GroupOrgDetail> result = new ArrayList<>();

            com.google.api.services.admin.directory.Directory.Groups.List listRequest =
                    service.groups().list()
                            .setCustomer("my_customer")
                            .setMaxResults(size)
                            .setOrderBy("email");
            if (pageToken != null && !pageToken.isBlank()) {
                listRequest.setPageToken(pageToken);
            }

            if (query != null && !query.trim().isEmpty()) {
                String cleanQuery = query.trim();
                if (cleanQuery.contains("@")) {
                    listRequest.setQuery("email:" + cleanQuery + "*");
                } else {
                    listRequest.setQuery("name:" + cleanQuery + "*");
                }
            }

            com.google.api.services.admin.directory.model.Groups groupsResult = listRequest.execute();
            List<com.google.api.services.admin.directory.model.Group> googleGroups =
                    groupsResult.getGroups();

            if (googleGroups != null) {
                for (com.google.api.services.admin.directory.model.Group group : googleGroups) {
                        try {
                            String groupEmail = group.getEmail();
                            if (groupEmail == null || groupEmail.isBlank()) continue;

                            int[] counts = countMembers(service, groupEmail, primaryDomain);
                            int total = counts[0];
                            int external = counts[1];
                            boolean externalAllowed = external > 0;
                            String risk = deriveRisk(external, total, externalAllowed);
                            List<String> tags = new ArrayList<>(deriveRiskTags(risk));

                            if (cloudIdentity != null) {
                                try {
                                    LookupGroupNameResponse lookup =
                                            cloudIdentity.groups().lookup()
                                                    .setGroupKeyId(groupEmail)
                                                    .execute();
                                    if (lookup != null && lookup.getName() != null) {
                                        com.google.api.services.cloudidentity.v1.model.Group ciGroup =
                                                cloudIdentity.groups().get(lookup.getName()).execute();
                                        if (ciGroup != null && ciGroup.getLabels() != null) {
                                            var labels = ciGroup.getLabels();
                                            if (labels.containsKey("cloudidentity.googleapis.com/groups.discussion_forum")) {
                                                tags.add("Mailing");
                                            }
                                            if (labels.containsKey("cloudidentity.googleapis.com/groups.security")) {
                                                tags.add("Security");
                                            }
                                        }
                                    }
                                } catch (Throwable t) {
                                    log.warn("Could not fetch Cloud Identity labels for {}: {}", groupEmail, t.getMessage());
                                }
                            }

                            String adminId = group.getId() != null ? group.getId() : "";
                            GroupSettingsDto settings = getGroupSettings(loggedInEmail, groupEmail);

                            result.add(new GroupOrgDetail(
                                    groupEmail,
                                    adminId,
                                    risk,
                                    tags,
                                    total,
                                    external,
                                    externalAllowed,
                                    settings.getWhoCanJoin(),
                                    settings.getWhoCanView()
                            ));
                        } catch (Throwable t) {
                            log.warn("Failed to process group: {}", t.getMessage());
                        }
                    }
                }

            return new GroupPageResponse(result, groupsResult.getNextPageToken());
        } catch (Exception e) {
            throw new RuntimeException("Failed to fetch groups from Google: " + e.getMessage());
        }
    }

    public GroupOverviewResponse getGroupsOverview(String loggedInEmail) {
        try {
            Set<String> scopes = Set.of(
                    DirectoryScopes.ADMIN_DIRECTORY_GROUP_READONLY,
                    DirectoryScopes.ADMIN_DIRECTORY_GROUP_MEMBER_READONLY
            );
            Directory service = directoryFactory.getDirectoryService(scopes, loggedInEmail);

            String primaryDomain = loggedInEmail.substring(loggedInEmail.indexOf('@') + 1);

            long totalGroups = 0;
            long groupsWithExternal = 0;
            long highRiskGroups = 0;
            long mediumRiskGroups = 0;
            long lowRiskGroups = 0;

            String pageToken = null;
            do {
                com.google.api.services.admin.directory.Directory.Groups.List listRequest =
                        service.groups().list()
                                .setCustomer("my_customer")
                                .setMaxResults(200)
                                .setOrderBy("email");
                if (pageToken != null && !pageToken.isBlank()) {
                    listRequest.setPageToken(pageToken);
                }

                com.google.api.services.admin.directory.model.Groups groupsResult = listRequest.execute();
                List<com.google.api.services.admin.directory.model.Group> googleGroups =
                        groupsResult.getGroups();

                if (googleGroups != null) {
                    for (com.google.api.services.admin.directory.model.Group group : googleGroups) {
                        try {
                            String groupEmail = group.getEmail();
                            if (groupEmail == null || groupEmail.isBlank()) continue;

                            int[] counts = countMembers(service, groupEmail, primaryDomain);
                            int total = counts[0];
                            int external = counts[1];
                            boolean externalAllowed = external > 0;
                            String risk = deriveRisk(external, total, externalAllowed);

                            totalGroups++;
                            if (external > 0 || externalAllowed) {
                                groupsWithExternal++;
                            }
                            switch (risk) {
                                case "HIGH" -> highRiskGroups++;
                                case "MEDIUM" -> mediumRiskGroups++;
                                default -> lowRiskGroups++;
                            }
                        } catch (Throwable t) {
                            log.warn("Failed to process group for overview: {}", t.getMessage());
                        }
                    }
                }
                pageToken = groupsResult.getNextPageToken();
            } while (pageToken != null);

            int securityScore = totalGroups == 0 ? 0
                    : (int) Math.round((lowRiskGroups * 100.0 + mediumRiskGroups * 60.0 + highRiskGroups * 20.0) / totalGroups);

            return new GroupOverviewResponse(
                    totalGroups,
                    groupsWithExternal,
                    highRiskGroups,
                    mediumRiskGroups,
                    lowRiskGroups,
                    securityScore
            );
        } catch (Exception e) {
            throw new RuntimeException("Failed to fetch groups overview from Google: " + e.getMessage());
        }
    }

    private int[] countMembers(Directory service, String groupEmail, String primaryDomain) {
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
        return new int[]{total, external};
    }

    public GroupSettingsDto getGroupSettings(String loggedInEmail, String groupEmail) {
        if (groupEmail == null || groupEmail.isBlank()) {
            return new GroupSettingsDto("—", "—");
        }
        try {
            Groupssettings settingsService = getGroupsSettingsService(loggedInEmail);
            Groups settings = settingsService.groups().get(groupEmail).execute();
            String whoCanJoin = mapWhoCanJoin(settings.getWhoCanJoin());
            String whoCanView = mapWhoCanViewMembership(settings.getWhoCanViewMembership());
            return new GroupSettingsDto(whoCanJoin, whoCanView);
        } catch (Throwable t) {
            log.warn("Could not fetch Groups Settings for {}: {}", groupEmail, t.getMessage());
            return new GroupSettingsDto("—", "—");
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

    private CloudIdentity getCloudIdentityService(String loggedInEmail) throws Exception {
        String pk = privateKey.replace("\\n", "\n");

        ServiceAccountCredentials credentials = ServiceAccountCredentials.newBuilder()
                .setClientEmail(clientEmail)
                .setPrivateKey(decodePrivateKey(pk))
                .setServiceAccountUser(loggedInEmail)
                .setScopes(Collections.singleton(CLOUD_IDENTITY_SCOPE))
                .build();

        return new CloudIdentity.Builder(
                GoogleNetHttpTransport.newTrustedTransport(),
                GsonFactory.getDefaultInstance(),
                new HttpCredentialsAdapter(credentials))
                .setApplicationName("CloudGuard")
                .build();
    }

    private String deriveRisk(int external, int total, boolean externalAllowed) {
        if (external > 0) return "HIGH";
        if (external < 0) return "MEDIUM";
        return "LOW";
    }

    private List<String> deriveRiskTags(String risk) {
        List<String> tags = new ArrayList<>();
        tags.add(switch (risk) {
            case "HIGH" -> "Hoog risico";
            case "MEDIUM" -> "Middel risico";
            default -> "Laag risico";
        });
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
