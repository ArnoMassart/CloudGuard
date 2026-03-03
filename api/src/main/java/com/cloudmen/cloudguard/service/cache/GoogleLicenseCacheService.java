package com.cloudmen.cloudguard.service.cache;

import com.cloudmen.cloudguard.dto.licenses.InactiveUser;
import com.cloudmen.cloudguard.dto.licenses.LicenseCacheEntry;
import com.cloudmen.cloudguard.dto.licenses.LicenseType;
import com.cloudmen.cloudguard.utility.DateTimeConverter;
import com.cloudmen.cloudguard.utility.GoogleApiFactory;
import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import com.google.api.client.util.DateTime;
import com.google.api.services.admin.directory.model.User;
import com.google.api.services.admin.directory.model.UserName;
import com.google.api.services.licensing.Licensing;
import com.google.api.services.licensing.LicensingScopes;
import com.google.api.services.licensing.model.LicenseAssignment;
import com.google.api.services.licensing.model.LicenseAssignmentList;
import org.springframework.stereotype.Service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.*;
import java.util.concurrent.TimeUnit;

@Service
public class GoogleLicenseCacheService {
    private static final Logger log = LoggerFactory.getLogger(GoogleLicenseCacheService.class);

    private final GoogleApiFactory googleApiFactory;
    private final GoogleUsersCacheService usersCacheService;

    private final Cache<String, LicenseCacheEntry> cache = Caffeine.newBuilder()
            .expireAfterWrite(6, TimeUnit.HOURS)
            .maximumSize(100)
            .build();

    // Mapping van SKU ID naar de mooie namen uit je mockup
    private static final Map<String, String> SKU_NAMES = Map.of(
            "1010020027", "Google Workspace Business Standard",
            "1010060001", "Google Workspace Enterprise",
            "1010330003", "Google Voice"
    );

    private final boolean isTestMode = true;

    public GoogleLicenseCacheService(GoogleApiFactory googleApiFactory, GoogleUsersCacheService usersCacheService) {
        this.googleApiFactory = googleApiFactory;
        this.usersCacheService = usersCacheService;
    }

    public void forceRefreshCache(String loggedInEmail) {
        cache.asMap().compute(loggedInEmail, this::fetchFromGoogle);
    }

    public LicenseCacheEntry getOrFetchLicenseData(String loggedInEmail) {
        return cache.get(loggedInEmail, email -> fetchFromGoogle(email, null));
    }

    private LicenseCacheEntry fetchFromGoogle(String loggedInEmail, LicenseCacheEntry fallback) {
        try {
            List<User> allUsers;
            List<LicenseAssignment> items;

            if (isTestMode) {
                // --- 1. GEBRUIK MOCK DATA I.P.V. GOOGLE API ---
                allUsers = createMockGoogleUsers();
                items = createMockAssignments();
            } else {
                Licensing licensingDirectory = googleApiFactory.getLicensingService(LicensingScopes.APPS_LICENSING, loggedInEmail);
                allUsers = usersCacheService.getOrFetchUsersData(loggedInEmail).allUsers();
                String domain = loggedInEmail.split("@")[1];

                LicenseAssignmentList assignments = licensingDirectory.licenseAssignments()
                        .listForProduct("Google-Apps", domain).execute();

                items = assignments.getItems() != null ? assignments.getItems() : Collections.emptyList();
            }

           List<LicenseType> licenseTypes = aggregateLicenseType(items);

           List<InactiveUser> inactiveUsers = findInactiveUsersWithLicenses(allUsers, items);

           return new LicenseCacheEntry(licenseTypes, inactiveUsers, System.currentTimeMillis());
        } catch (Exception e) {
            if (fallback != null) {
                log.error("Google API faalde! Terugvallen op oude cache: {}", e.getMessage());
                return fallback;
            }
            throw new RuntimeException("Fout bij ophalen Google licentie data, en geen cache beschikbaar: " + e.getMessage());
        }
    }

    private List<LicenseType> aggregateLicenseType(List<LicenseAssignment> assignments) {
        Map<String, Integer> counts = new HashMap<>();
        assignments.forEach(a -> counts.merge(a.getSkuId(), 1, Integer::sum));

        List<LicenseType> types = new ArrayList<>();
        SKU_NAMES.forEach((skuId, name) -> {
            int assigned = counts.getOrDefault(skuId, 0);
            int total = (skuId.equals("1010020027")) ? 60 : (skuId.equals("1010060001") ? 25 : 15);
            types.add(new LicenseType(skuId, name, total, assigned, total - assigned));
        });

        return types;
    }

    private List<InactiveUser> findInactiveUsersWithLicenses(List<User> users, List<LicenseAssignment> assignments) {
        List<InactiveUser> result = new ArrayList<>();
        Instant ninetyDaysAgo = Instant.now().minus(90, ChronoUnit.DAYS);

        Map<String, String> userToSku = new HashMap<>();
        assignments.forEach(a -> userToSku.put(a.getUserId(), SKU_NAMES.getOrDefault(a.getSkuId(), "Unknown")));

        for (User user: users) {
            if (user.getLastLoginTime() == null || !userToSku.containsKey(user.getPrimaryEmail())) continue;

            Instant lastLogin = Instant.parse(user.getLastLoginTime().toString());
            if (lastLogin.isBefore(ninetyDaysAgo)) {
                result.add(new InactiveUser(
                        user.getPrimaryEmail(),
                        DateTimeConverter.convertToTimeAgo(user.getLastLoginTime()),
                        userToSku.get(user.getPrimaryEmail()),
                        ChronoUnit.DAYS.between(lastLogin, Instant.now())
                ));
            }
        }

        return result;
    }

    private List<User> createMockGoogleUsers() {
        List<User> list = new ArrayList<>();
        long now = System.currentTimeMillis();
        long dayInMillis = 86400000L;

        // 1. Inactieve gebruiker (200 dagen - komt uit op ~15-08-2025)
        User u1 = new User();
        u1.setPrimaryEmail("lisa.smit@bedrijf.nl");
        UserName n1 = new UserName(); n1.setFullName("Lise Smit"); u1.setName(n1);
        u1.setLastLoginTime(new DateTime(now - (200 * dayInMillis)));
        list.add(u1);

        // 2. Inactieve gebruiker (162 dagen - komt uit op ~22-09-2025)
        User u2 = new User();
        u2.setPrimaryEmail("pieter.devries@bedrijf.nl");
        UserName n2 = new UserName(); n2.setFullName("Pieter Devries"); u2.setName(n2);
        u2.setLastLoginTime(new DateTime(now - (162 * dayInMillis)));
        list.add(u2);

        // 3. Inactieve gebruiker (236 dagen - komt uit op ~10-07-2025)
        User u3 = new User();
        u3.setPrimaryEmail("tom.klaassen@bedrijf.nl");
        UserName n3 = new UserName(); n3.setFullName("Tom Klaassen"); u3.setName(n3);
        u3.setLastLoginTime(new DateTime(now - (236 * dayInMillis)));
        list.add(u3);

        // 4. Actieve gebruiker (5 dagen - deze mag NIET in je tabel verschijnen!)
        User u4 = new User();
        u4.setPrimaryEmail("emma.devries@bedrijf.nl");
        UserName n4 = new UserName(); n4.setFullName("Emma Devries"); u4.setName(n4);
        u4.setLastLoginTime(new DateTime(now - (5 * dayInMillis)));
        list.add(u4);

        return list;
    }

    private List<LicenseAssignment> createMockAssignments() {
        List<LicenseAssignment> list = new ArrayList<>();

        // Koppel de gebruikers aan hun licenties via de SKU ID's
        LicenseAssignment a1 = new LicenseAssignment();
        a1.setUserId("oud.account1@bedrijf.nl");
        a1.setSkuId("1010020027"); // Business Standard
        list.add(a1);

        LicenseAssignment a2 = new LicenseAssignment();
        a2.setUserId("oud.account2@bedrijf.nl");
        a2.setSkuId("1010060001"); // Enterprise
        list.add(a2);

        LicenseAssignment a3 = new LicenseAssignment();
        a3.setUserId("oud.account3@bedrijf.nl");
        a3.setSkuId("1010020027"); // Business Standard
        list.add(a3);

        // De actieve gebruiker heeft ook een licentie
        LicenseAssignment a4 = new LicenseAssignment();
        a4.setUserId("actief.account@bedrijf.nl");
        a4.setSkuId("1010330003"); // Google Voice
        list.add(a4);

        return list;
    }
}
