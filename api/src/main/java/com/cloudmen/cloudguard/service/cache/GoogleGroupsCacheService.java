package com.cloudmen.cloudguard.service.cache;

import com.cloudmen.cloudguard.dto.groups.CachedGroupItem;
import com.cloudmen.cloudguard.dto.groups.GroupCacheEntry;
import com.cloudmen.cloudguard.dto.groups.GroupOrgDetail;
import com.cloudmen.cloudguard.dto.users.UserCacheEntry;
import com.cloudmen.cloudguard.utility.GoogleApiFactory;
import com.google.api.client.googleapis.javanet.GoogleNetHttpTransport;
import com.google.api.client.json.gson.GsonFactory;
import com.google.api.services.admin.directory.Directory;
import com.google.api.services.admin.directory.DirectoryScopes;
import com.google.api.services.admin.directory.model.Member;
import com.google.api.services.admin.directory.model.Members;
import com.google.api.services.cloudidentity.v1.CloudIdentity;
import com.google.api.services.cloudidentity.v1.model.LookupGroupNameResponse;
import com.google.api.services.groupssettings.Groupssettings;
import com.google.auth.http.HttpCredentialsAdapter;
import com.google.auth.oauth2.ServiceAccountCredentials;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

import static com.cloudmen.cloudguard.utility.GoogleServiceHelperMethods.decodePrivateKey;

@Service
public class GoogleGroupsCacheService {
    private static final Logger log = LoggerFactory.getLogger(GoogleGroupsCacheService.class);

    private final GoogleApiFactory googleApiFactory;

    private final Map<String, GroupCacheEntry> cache = new ConcurrentHashMap<>();
    private static final long CACHE_TTL_MS = 3600000L;

    private static final String CLOUD_IDENTITY_SCOPE = "https://www.googleapis.com/auth/cloud-identity.groups.readonly";
    private static final String GROUPS_SETTINGS_SCOPE = "https://www.googleapis.com/auth/apps.groups.settings";


    @Value("${google.api.client-email}")
    private String clientEmail;

    @Value("${google.api.private-key}")
    private String privateKey;

    public GoogleGroupsCacheService(GoogleApiFactory googleApiFactory) {
        this.googleApiFactory = googleApiFactory;
    }

    public void forceRefreshCache(String loggedInEmail) {
        cache.compute(loggedInEmail, this::fetchFromGoogle);
    }

    public GroupCacheEntry getOrFetchGroupData(String loggedInEmail) {
        return cache.compute(loggedInEmail, (email, entry) -> {
            long now = System.currentTimeMillis();

            if (entry != null && (now - entry.timestamp() < CACHE_TTL_MS)) {
                return entry;
            }

            return fetchFromGoogle(email, entry);
        });
    }

    private GroupCacheEntry fetchFromGoogle(String loggedInEmail, GroupCacheEntry fallbackEntry) {
        try {
            log.info("Ophalen LIVE Groep data van Google voor: {}", loggedInEmail);

            Set<String> scopes = Set.of(
                    DirectoryScopes.ADMIN_DIRECTORY_GROUP_READONLY,
                    DirectoryScopes.ADMIN_DIRECTORY_GROUP_MEMBER_READONLY
            );
            Directory service = googleApiFactory.getDirectoryService(scopes, loggedInEmail);

            CloudIdentity cloudIdentity = null;
            try {
                cloudIdentity = getCloudIdentityService(loggedInEmail);
            } catch (Throwable t) {
                log.warn("Cloud Identity API unavailable", t);
            }

            String primaryDomain = loggedInEmail.substring(loggedInEmail.indexOf('@') + 1);

            // A. Haal alle groepen en details op
            List<CachedGroupItem> allMappedGroups = fetchAllGroups(service, cloudIdentity, primaryDomain);

            return new GroupCacheEntry(allMappedGroups, System.currentTimeMillis());

        } catch (Exception e) {
            if (fallbackEntry != null) {
                log.error("Google API faalde! Terugvallen op oude cache: {}", e.getMessage());
                return fallbackEntry;
            }
            throw new RuntimeException("Fout bij ophalen Google Groups, en geen cache beschikbaar: " + e.getMessage());
        }
    }

    private List<CachedGroupItem> fetchAllGroups(Directory service, CloudIdentity cloudIdentity, String primaryDomain) throws IOException {
        List<CachedGroupItem> allMappedGroups = new ArrayList<>();
        String pageToken = null;

        do {
            com.google.api.services.admin.directory.Directory.Groups.List listRequest =
                    service.groups().list().setCustomer("my_customer").setMaxResults(200).setOrderBy("email");

            if (pageToken != null) listRequest.setPageToken(pageToken);
            com.google.api.services.admin.directory.model.Groups groupsResult = listRequest.execute();

            if (groupsResult.getGroups() != null) {
                for (com.google.api.services.admin.directory.model.Group group : groupsResult.getGroups()) {
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
                            LookupGroupNameResponse lookup = cloudIdentity.groups().lookup().setGroupKeyId(groupEmail).execute();
                            if (lookup != null && lookup.getName() != null) {
                                com.google.api.services.cloudidentity.v1.model.Group ciGroup = cloudIdentity.groups().get(lookup.getName()).execute();
                                if (ciGroup != null && ciGroup.getLabels() != null) {
                                    var labels = ciGroup.getLabels();
                                    if (labels.containsKey("cloudidentity.googleapis.com/groups.discussion_forum")) tags.add("Mailing");
                                    if (labels.containsKey("cloudidentity.googleapis.com/groups.security")) tags.add("Security");
                                }
                            }
                        } catch (Throwable t) {
                            log.warn("Cloud Identity tags mislukt voor {}: {}", groupEmail, t.getMessage());
                        }
                    }

                    String adminId = group.getId() != null ? group.getId() : "";
                    String groupName = group.getName() != null ? group.getName() : "";

                    GroupOrgDetail detail = new GroupOrgDetail(
                            groupEmail, adminId, risk, tags, total, external, externalAllowed, "—", "—"
                    );

                    allMappedGroups.add(new CachedGroupItem(groupName, groupEmail, detail));
                }
            }
            pageToken = groupsResult.getNextPageToken();
        } while (pageToken != null);

        return allMappedGroups;
    }

    private int[] countMembers(Directory service, String groupEmail, String primaryDomain) {
        int total = 0; int external = 0;
        try {
            String memberPageToken = null;
            do {
                Members members = service.members().list(groupEmail).setMaxResults(200).setPageToken(memberPageToken).execute();
                if (members.getMembers() != null) {
                    for (Member member : members.getMembers()) {
                        if ("USER".equals(member.getType())) {
                            total++;
                            if (member.getEmail() != null && !member.getEmail().endsWith("@" + primaryDomain)) external++;
                        }
                    }
                }
                memberPageToken = members.getNextPageToken();
            } while (memberPageToken != null);
        } catch (Exception e) {}
        return new int[]{total, external};
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

    private CloudIdentity getCloudIdentityService(String loggedInEmail) throws Exception {
        String pk = privateKey.replace("\\n", "\n");
        ServiceAccountCredentials credentials = ServiceAccountCredentials.newBuilder()
                .setClientEmail(clientEmail).setPrivateKey(decodePrivateKey(pk)).setServiceAccountUser(loggedInEmail)
                .setScopes(Collections.singleton(CLOUD_IDENTITY_SCOPE)).build();
        return new CloudIdentity.Builder(GoogleNetHttpTransport.newTrustedTransport(), GsonFactory.getDefaultInstance(), new HttpCredentialsAdapter(credentials))
                .setApplicationName("CloudGuard").build();
    }

    public Groupssettings getGroupsSettingsService(String loggedInEmail) throws Exception {
        String pk = privateKey.replace("\\n", "\n");
        ServiceAccountCredentials credentials = ServiceAccountCredentials.newBuilder()
                .setClientEmail(clientEmail).setPrivateKey(decodePrivateKey(pk)).setServiceAccountUser(loggedInEmail)
                .setScopes(Collections.singleton(GROUPS_SETTINGS_SCOPE)).build();
        return new Groupssettings.Builder(GoogleNetHttpTransport.newTrustedTransport(), GsonFactory.getDefaultInstance(), new HttpCredentialsAdapter(credentials))
                .setApplicationName("CloudGuard").build();
    }
}
