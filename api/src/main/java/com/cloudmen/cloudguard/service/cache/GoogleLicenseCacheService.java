package com.cloudmen.cloudguard.service.cache;

import com.cloudmen.cloudguard.dto.licenses.InactiveUser;
import com.cloudmen.cloudguard.dto.licenses.LicenseCacheEntry;
import com.cloudmen.cloudguard.dto.licenses.LicenseType;
import com.cloudmen.cloudguard.exception.GoogleWorkspaceSyncException;
import com.cloudmen.cloudguard.utility.DateTimeConverter;
import com.cloudmen.cloudguard.utility.GoogleApiFactory;
import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import com.google.api.services.admin.directory.model.User;
import com.google.api.services.licensing.Licensing;
import com.google.api.services.licensing.LicensingScopes;
import com.google.api.services.licensing.model.LicenseAssignment;
import com.google.api.services.licensing.model.LicenseAssignmentList;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

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

            List<String> productIds = List.of(
                    "Google-Apps",
                    "101001",
                    "101005",
                    "101031",
                    "101037"
            );
            List<LicenseAssignment> allItems = new ArrayList<>();

            for (String productId : productIds) {
                allItems.addAll(fetchAssignmentsForProduct(licensingDirectory, productId, domain));
            }

            List<User> allUsers = usersCacheService.getOrFetchUsersData(loggedInEmail).allUsers();

            List<LicenseType> licenseTypes = mapAssignmentsToLicenseTypes(allItems);

            List<InactiveUser> inactiveUsers = findInactiveUsersWithLicenses(allUsers, allItems);

            return new LicenseCacheEntry(licenseTypes, inactiveUsers, System.currentTimeMillis());
        } catch (Exception e) {
            if (fallback != null) {
                log.error("Google API faalde! Terugvallen op oude cache: {}", e.getMessage());
                return fallback;
            }
            throw new GoogleWorkspaceSyncException("Fout bij ophalen Google licentie data: " + e.getMessage());
        }
    }

    private List<LicenseAssignment> fetchAssignmentsForProduct(Licensing licensingDirectory, String productId, String domain) {
        try {
            LicenseAssignmentList assignments = licensingDirectory.licenseAssignments()
                    .listForProduct(productId, domain).execute();

            if (assignments.getItems() != null) {
                return assignments.getItems();
            }
        } catch (Exception e) {
            log.warn("Kon geen data ophalen voor Product ID: {} - {}", productId, e.getMessage());
        }

        return Collections.emptyList();
    }

    private List<LicenseType> mapAssignmentsToLicenseTypes(List<LicenseAssignment> assignments) {
        Map<String, Integer> counts = new HashMap<>();
        Map<String, String> names = new HashMap<>();

        for (LicenseAssignment assignment : assignments) {
            String skuId = assignment.getSkuId();

            String skuName = (assignment.getSkuName() != null) ? assignment.getSkuName() : skuId;

            counts.merge(skuId, 1, Integer::sum);
            names.putIfAbsent(skuId, skuName);
        }

        List<LicenseType> results = new ArrayList<>();

        for (Map.Entry<String, Integer> entry : counts.entrySet()) {
            String skuId = entry.getKey();
            int assigned = entry.getValue();
            String skuName = names.get(skuId);

            results.add(new LicenseType(
                    skuId,
                    skuName,
                    assigned
            ));
        }

        return results;
    }

    private List<InactiveUser> findInactiveUsersWithLicenses(List<User> users, List<LicenseAssignment> assignments) {
        List<InactiveUser> result = new ArrayList<>();
        Instant ninetyDaysAgo = Instant.now().minus(90, ChronoUnit.DAYS);

        Map<String, String> userToSku = new HashMap<>();
        assignments.forEach(a -> userToSku.put(a.getUserId(), a.getSkuName()));

        for (User user : users) {
            String email = user.getPrimaryEmail();
            String skuName = userToSku.get(email);

            if (user.getLastLoginTime() != null && skuName != null && !skuName.contains("Free")) {
                Instant lastLogin = Instant.parse(user.getLastLoginTime().toString());

                if (lastLogin.isBefore(ninetyDaysAgo)) {
                    result.add(new InactiveUser(
                            user.getPrimaryEmail(),
                            DateTimeConverter.convertToTimeAgo(user.getLastLoginTime()),
                            userToSku.get(user.getPrimaryEmail()),
                            user.getIsEnrolledIn2Sv(),
                            ChronoUnit.DAYS.between(lastLogin, Instant.now())
                    ));
                }
            }
        }

        return result;
    }
}
