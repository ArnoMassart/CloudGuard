package com.cloudmen.cloudguard.service.cache;

import com.cloudmen.cloudguard.dto.organization.OrgUnitCacheEntry;
import com.cloudmen.cloudguard.utility.GoogleApiFactory;
import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
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
import java.util.concurrent.TimeUnit;

@Service
public class GoogleOrgUnitCacheService {
    private static final Logger log = LoggerFactory.getLogger(GoogleOrgUnitCacheService.class);

    private final GoogleApiFactory directoryFactory;

    // --- CACHE CONFIGURATIE ---
    private final Cache<String, OrgUnitCacheEntry> cache = Caffeine.newBuilder()
            .expireAfterWrite(24, TimeUnit.HOURS)
            .maximumSize(100)
            .build();

    public GoogleOrgUnitCacheService(GoogleApiFactory directoryFactory) {
        this.directoryFactory = directoryFactory;
    }

    public void forceRefreshCache(String adminEmail) {
        cache.asMap().compute(adminEmail, this::fetchFromGoogle);
    }

    public OrgUnitCacheEntry getOrFetchOrgUnitData(String loggedInEmail) {
        return cache.get(loggedInEmail, email -> fetchFromGoogle(email, null));
    }

    private OrgUnitCacheEntry fetchFromGoogle(String loggedInEmail, OrgUnitCacheEntry fallbackEntry) {
        try {
            log.info("Ophalen LIVE OrgUnit data van Google voor: {}", loggedInEmail);
            Set<String> scopes = Set.of(
                    DirectoryScopes.ADMIN_DIRECTORY_ORGUNIT_READONLY,
                    DirectoryScopes.ADMIN_DIRECTORY_USER_READONLY
            );
            Directory directory = directoryFactory.getDirectoryService(scopes, loggedInEmail);

            // 1. Haal de ruwe OrgUnits op
            List<OrgUnit> allOrgUnits = fetchAllOrgUnits(directory);

            // 2. Haal de gebruikerstellingen op
            Map<String, Integer> userCountsByOu = fetchAllUserCounts(directory);

            return new OrgUnitCacheEntry(allOrgUnits, userCountsByOu, System.currentTimeMillis());

        } catch (Exception e) {
            if (fallbackEntry != null) {
                log.error("Google API faalde! Terugvallen op oude cache: {}", e.getMessage());
                return fallbackEntry;
            }
            log.error("Fout bij ophalen Google OrgUnits: {}", e.getMessage(), e);
            return new OrgUnitCacheEntry(new ArrayList<>(), new HashMap<>(), System.currentTimeMillis());
        }
    }

    private List<OrgUnit> fetchAllOrgUnits(Directory directory) throws IOException {
        OrgUnits response = directory.orgunits().list("my_customer").setType("all").execute();
        return (response != null && response.getOrganizationUnits() != null)
                ? response.getOrganizationUnits()
                : new ArrayList<>();
    }

    private Map<String, Integer> fetchAllUserCounts(Directory directory) {
        Map<String, Integer> counts = new HashMap<>();
        try {
            String pageToken = null;
            do {
                Directory.Users.List request = directory.users().list()
                        .setCustomer("my_customer")
                        .setMaxResults(500)
                        // CRUCIAAL: Haal alleen ID en orgUnitPath op voor extreme snelheid!
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
