package com.cloudmen.cloudguard.service.policy;

import com.cloudmen.cloudguard.dto.organization.OrgUnitPolicyDto;
import com.cloudmen.cloudguard.utility.GoogleApiFactory;
import com.google.api.services.admin.directory.Directory;
import com.google.api.services.admin.directory.model.User;
import com.google.api.services.admin.directory.model.Users;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

@Order(1)
@Component
public class TSVPolicyProvider implements OrgUnitPolicyProvider {
    private static final Logger log = LoggerFactory.getLogger(TSVPolicyProvider.class);

    private static final String DIRECTORY_USER_SECURITY_SCOPE = "https://www.googleapis.com/auth/admin.directory.user.security";
    private static final String DIRECTORY_USER_READONLY_SCOPE = "https://www.googleapis.com/auth/admin.directory.user.readonly";
    private static final String SETTINGS_LINK_TEXT = "Klik hier om deze instellingen aan te passen";

    private final GoogleApiFactory directoryFactory;

    private final Map<String, CacheEntry> cache = new ConcurrentHashMap<>();
    private static final long CACHE_TTL_MS = 3600000L;

    public TSVPolicyProvider(GoogleApiFactory directoryFactory) {
        this.directoryFactory = directoryFactory;
    }

    @Override public String key() {return "2sv_adoption";}

    public void forceRefreshCache(String adminEmail) {
        cache.compute(adminEmail, this::fetchAllUsersFromGoogle);
    }

    @Override
    public OrgUnitPolicyDto fetch(String loggedInEmail, String orgUnitPath) throws Exception {
        Map<String, TsvStats> allStats = getOrFetchTsvData(loggedInEmail);

        String normalizedPath = (orgUnitPath == null || orgUnitPath.isBlank()) ? "/" : orgUnitPath.trim();
        TsvStats stats = allStats.getOrDefault(normalizedPath, new TsvStats(0, 0));

        int total = stats.total();
        int without2sv = stats.without2sv();

        String status;
        String css;

        String baseExplanation = "Deze beleidsregel toont de adoptie van tweestapsverificatie (2SV) onder gebruikers in deze organisatie-eenheid. Tweestapsverificatie voegt een extra beveiligingslaag toe aan het inloggen.";
        String description;

        if (total == 0) {
            status = "Geen gebruikers";
            css = "bg-slate-100 text-slate-700";
            description = "Er zijn geen gebruikers om te controleren";
        } else if (without2sv == 0) {
            status = "OK (" + total + "/" + total + ")";
            css = "bg-green-100 text-green-800";
            description = "Alle gebruikers hebben 2SV ingeschakeld";
        } else {
            status = "Risico (" + without2sv + "/" + total + " zonder 2SV)";
            css = "bg-amber-100 text-amber-800";
            description = "Sommige gebruikers hebben geen 2SV geactiveerd";
        }

        return new OrgUnitPolicyDto(
                key(),
                "Tweestapsverificatie (2SV) adoptie",
                description,
                status,
                css,
                baseExplanation,
                null,
                false,
                SETTINGS_LINK_TEXT,
                "https://admin.google.com/u/1/ac/security/2sv",
                null
        );
    }

    private Map<String, TsvStats> getOrFetchTsvData(String loggedInEmail) {
        return cache.compute(loggedInEmail, (email, existingEntry) -> {
            long now = System.currentTimeMillis();
            if (existingEntry != null && (now - existingEntry.timestamp() < CACHE_TTL_MS)) {
                return existingEntry;
            }
            return fetchAllUsersFromGoogle(email, existingEntry);
        }).statsMap();
    }

    private CacheEntry fetchAllUsersFromGoogle(String loggedInEmail, CacheEntry fallbackEntry) {
        try {
            log.info("Ophalen LIVE 2SV data van Google voor: {}", loggedInEmail);
            Set<String> scopes = Set.of(DIRECTORY_USER_READONLY_SCOPE, DIRECTORY_USER_SECURITY_SCOPE);
            Directory directory = directoryFactory.getDirectoryService(scopes, loggedInEmail);

            Map<String, TsvStats> statsMap = new HashMap<>();
            String pageToken = null;

            // Haal ALLE gebruikers van het hele bedrijf op (in grote batches van 500)
            do {
                Directory.Users.List req = directory.users().list()
                        .setCustomer("my_customer")
                        .setMaxResults(500)
                        // Vraag alléén de velden op die we nodig hebben voor extreme snelheid!
                        .setFields("nextPageToken, users(orgUnitPath, isEnrolledIn2Sv)");

                if (pageToken != null) req.setPageToken(pageToken);

                Users users = req.execute();
                if (users.getUsers() != null) {
                    for (User u : users.getUsers()) {
                        String ouPath = (u.getOrgUnitPath() != null && !u.getOrgUnitPath().isBlank()) ? u.getOrgUnitPath().trim() : "/";

                        Boolean enrolled = u.getIsEnrolledIn2Sv();
                        boolean hasNo2sv = (enrolled == null || !enrolled);

                        // Haal huidige stats voor deze OU op, of maak nieuwe aan
                        TsvStats currentStats = statsMap.getOrDefault(ouPath, new TsvStats(0, 0));
                        statsMap.put(ouPath, new TsvStats(
                                currentStats.total() + 1,
                                currentStats.without2sv() + (hasNo2sv ? 1 : 0)
                        ));
                    }
                }
                pageToken = users.getNextPageToken();
            } while (pageToken != null && !pageToken.isBlank());

            return new CacheEntry(statsMap, System.currentTimeMillis());

        } catch (Exception e) {
            if (fallbackEntry != null) {
                log.error("Google API faalde! Terugvallen op oude 2SV cache: {}", e.getMessage());
                return fallbackEntry;
            }
            log.error("Kon 2SV data niet ophalen: {}", e.getMessage());
            return new CacheEntry(new HashMap<>(), System.currentTimeMillis());
        }
    }

    private record CacheEntry(Map<String, TsvStats> statsMap, long timestamp) {}
    private record TsvStats(int total, int without2sv) {}
}
