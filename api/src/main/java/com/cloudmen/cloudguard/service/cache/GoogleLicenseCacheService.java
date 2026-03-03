package com.cloudmen.cloudguard.service.cache;

import com.cloudmen.cloudguard.dto.licenses.InactiveUser;
import com.cloudmen.cloudguard.dto.licenses.LicenseCacheEntry;
import com.cloudmen.cloudguard.dto.licenses.LicenseType;
import com.cloudmen.cloudguard.utility.DateTimeConverter;
import com.cloudmen.cloudguard.utility.GoogleApiFactory;
import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import com.google.api.client.util.DateTime;
import com.google.api.services.admin.directory.Directory;
import com.google.api.services.admin.directory.DirectoryScopes;
import com.google.api.services.admin.directory.model.User;
import com.google.api.services.admin.directory.model.UserName;
import com.google.api.services.licensing.Licensing;
import com.google.api.services.licensing.LicensingScopes;
import com.google.api.services.licensing.model.LicenseAssignment;
import com.google.api.services.licensing.model.LicenseAssignmentList;
import org.springframework.stereotype.Service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.context.annotation.ApplicationScope;

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
    private static final Map<String, String> GOOGLE_SKU_CATALOG = Map.of(
            "1010020027", "Google Workspace Business Standard",
            "1010060001", "Google Workspace Enterprise",
            "1010330003", "Google Voice",
            "1010020025", "Google Workspace Business Plus" // Jouw nieuwe licentie!
    );

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
            log.info("Ophalen LIVE License data van Google voor: {}", loggedInEmail);
            Licensing licensingDirectory = googleApiFactory.getLicensingService(LicensingScopes.APPS_LICENSING, loggedInEmail);
            String domain = loggedInEmail.split("@")[1];

            LicenseAssignmentList assignments = licensingDirectory.licenseAssignments()
                    .listForProduct("Google-Apps", domain).execute();

            List<LicenseAssignment> items = assignments.getItems() != null ? assignments.getItems() : Collections.emptyList();

            List<User> allUsers = usersCacheService.getOrFetchUsersData(loggedInEmail).allUsers();

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

    private record Sku(
            String skuId,
            String skuName
    ) {}

    private List<LicenseType> aggregateLicenseType(List<LicenseAssignment> assignments) {
        Map<Sku, Integer> assignedCounts = new HashMap<>();
        for (LicenseAssignment assignment : assignments) {
            assignedCounts.merge(new Sku(assignment.getSkuId(), assignment.getSkuName()), 1, Integer::sum);
        }

        List<LicenseType> types = new ArrayList<>();

        for (Map.Entry<Sku, Integer> entry : assignedCounts.entrySet()) {
            String skuId = entry.getKey().skuId;
            String skuName = entry.getKey().skuName;
            int assigned = entry.getValue();

            int totalPurchased = fetchTotalSeatsFromBillingSystem(skuId, assigned);

            int available = totalPurchased - assigned;

            types.add(new LicenseType(skuId, skuName, totalPurchased, assigned, available));
        }

        return types;
    }

    private List<InactiveUser> findInactiveUsersWithLicenses(List<User> users, List<LicenseAssignment> assignments) {
        List<InactiveUser> result = new ArrayList<>();
        Instant ninetyDaysAgo = Instant.now().minus(90, ChronoUnit.DAYS);

        Map<String, String> userToSku = new HashMap<>();
        assignments.forEach(a -> userToSku.put(a.getUserId(), a.getSkuName()));

        for (User user : users) {
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

    // Dit is de 'Connector naar het billing systeem' uit je opdracht!
    private int fetchTotalSeatsFromBillingSystem(String skuId, int currentlyAssigned) {
        // In het echt doe je hier een API-call naar bijvoorbeeld Exact, AFAS of jullie interne CRM.
        // Voorbeeld logica: Het facturatiesysteem zegt dat er 60 gekocht zijn.
        if (skuId.equals("1010020027")) return 60;

        // Fallback: Als het billingsysteem even niet werkt, gaan we er vanuit dat er
        // in ieder geval genoeg licenties zijn gekocht voor de mensen die er een hebben, plus 5 reserve.
        return currentlyAssigned + 5;
    }
}
