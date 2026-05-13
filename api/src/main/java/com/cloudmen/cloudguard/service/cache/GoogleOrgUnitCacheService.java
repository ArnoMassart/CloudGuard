package com.cloudmen.cloudguard.service.cache;

import com.cloudmen.cloudguard.exception.GoogleWorkspaceSyncException;
import com.cloudmen.cloudguard.dto.organization.OrgUnitCacheEntry;
import com.cloudmen.cloudguard.service.OrganizationService;
import com.cloudmen.cloudguard.service.user.UserService;
import com.cloudmen.cloudguard.utility.GoogleApiFactory;
import com.google.api.services.admin.directory.Directory;
import com.google.api.services.admin.directory.DirectoryScopes;
import com.google.api.services.admin.directory.model.OrgUnit;
import com.google.api.services.admin.directory.model.OrgUnits;
import com.google.api.services.admin.directory.model.User;
import com.google.api.services.admin.directory.model.Users;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.util.*;

/**
 * Caches Google Workspace <strong>organizational units</strong> and a lightweight user→OU tally for each Directory
 * {@code orgUnitPath}. Uses Admin SDK {@code orgunits.list} plus a partial {@code users.list} projection; entries expire
 * after twenty-four hours via {@link AbstractGoogleWorkspaceCacheService}.
 */
@Service
public class GoogleOrgUnitCacheService extends AbstractGoogleWorkspaceCacheService<OrgUnitCacheEntry> {
    private static final Logger log = LoggerFactory.getLogger(GoogleOrgUnitCacheService.class);

    private final GoogleApiFactory directoryFactory;

    /**
     * @param directoryFactory      builds Directory clients scoped to the impersonated admin
     * @param userService           resolves workspace admin for the logged-in caller
     * @param organizationService   tenant lookup backing admin resolution
     */
    public GoogleOrgUnitCacheService(GoogleApiFactory directoryFactory, UserService userService, OrganizationService organizationService) {
        super(userService, organizationService, 24);
        this.directoryFactory = directoryFactory;
    }

    /**
     * Loads all OUs for {@code my_customer} and aggregates user counts per {@link User#getOrgUnitPath()} (defaulting
     * missing paths to {@code "/"}). Returns {@code fallbackEntry} when Google errors and a stale entry exists.
     */
    @Override
    protected OrgUnitCacheEntry fetchFromGoogle(String adminEmail, OrgUnitCacheEntry fallbackEntry) {
        try {
            log.info("Ophalen LIVE Org Units data van Google. Impersonatie via Admin: {}", adminEmail);
            Set<String> scopes = Set.of(
                    DirectoryScopes.ADMIN_DIRECTORY_ORGUNIT_READONLY,
                    DirectoryScopes.ADMIN_DIRECTORY_USER_READONLY
            );
            Directory directory = directoryFactory.getDirectoryService(scopes, adminEmail);

            List<OrgUnit> allOrgUnits = fetchAllOrgUnits(directory);

            Map<String, Integer> userCountsByOu = fetchAllUserCounts(directory);

            return new OrgUnitCacheEntry(allOrgUnits, userCountsByOu, System.currentTimeMillis());

        } catch (Exception e) {
            if (fallbackEntry != null) {
                log.error("Google API faalde! Terugvallen op oude cache: {}", e.getMessage());
                return fallbackEntry;
            }
            throw new GoogleWorkspaceSyncException(
                    "Fout bij ophalen Google OrgUnits, en geen cache beschikbaar: " + e.getMessage());
        }
    }

    /** All units returned by {@code orgunits.list} with {@code type=all}. */
    private List<OrgUnit> fetchAllOrgUnits(Directory directory) throws IOException {
        OrgUnits response = directory.orgunits().list("my_customer").setType("all").execute();
        return (response != null && response.getOrganizationUnits() != null)
                ? response.getOrganizationUnits()
                : new ArrayList<>();
    }

    /**
     * Pages through workspace users counting only {@code orgUnitPath}; uses {@code fields} projection for throughput.
     *
     * @return map from path string (e.g. {@code "/Sales/EMEA"}) to number of users with that path
     */
    private Map<String, Integer> fetchAllUserCounts(Directory directory) {
        Map<String, Integer> counts = new HashMap<>();
        try {
            String pageToken = null;
            do {
                Directory.Users.List request = directory.users().list()
                        .setCustomer("my_customer")
                        .setMaxResults(500)
                        .setFields("nextPageToken, users(id, orgUnitPath)");

                if (pageToken != null) request.setPageToken(pageToken);

                Users response = request.execute();

                if (response.getUsers() != null) {
                    for (User user : response.getUsers()) {
                        String path = user.getOrgUnitPath() != null ? user.getOrgUnitPath() : "/";
                        counts.put(path, counts.getOrDefault(path, 0) + 1);
                    }
                }
                pageToken = response.getNextPageToken();
            } while (pageToken != null && !pageToken.isBlank());

        } catch (Exception e) {
            log.warn("Kon globale gebruikers niet tellen: {}", e.getMessage());
        }
        return counts;
    }
}
