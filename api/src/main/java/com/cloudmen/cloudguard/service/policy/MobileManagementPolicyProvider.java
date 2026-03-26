package com.cloudmen.cloudguard.service.policy;

import com.cloudmen.cloudguard.dto.organization.OrgUnitPolicyDto;
import com.cloudmen.cloudguard.service.cache.GoogleDeviceCacheService;
import com.cloudmen.cloudguard.utility.GoogleApiFactory;
import com.google.api.services.admin.directory.Directory;
import com.google.api.services.admin.directory.DirectoryScopes;
import com.google.api.services.admin.directory.model.MobileDevice;
import com.google.api.services.admin.directory.model.User;
import com.google.api.services.admin.directory.model.Users;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.MessageSource;
import org.springframework.context.i18n.LocaleContextHolder;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Policy provider for mobile device management levels per OU.
 * Infers Basic vs Advanced per device from Directory API device fields
 * (Advanced devices report encryptionStatus, devicePasswordStatus, applications).
 * Aggregates per OU by mapping device owners to their orgUnitPath.
 */
@Order(2)
@Component
public class MobileManagementPolicyProvider implements OrgUnitPolicyProvider {

    private static final Logger log = LoggerFactory.getLogger(MobileManagementPolicyProvider.class);
    private static final String DIRECTORY_USER_READONLY = "https://www.googleapis.com/auth/admin.directory.user.readonly";

    private final GoogleDeviceCacheService deviceCache;
    private final GoogleApiFactory directoryFactory;

    private final Map<String, CacheEntry> cache = new ConcurrentHashMap<>();
    private static final long CACHE_TTL_MS = 3600000L;
    private final MessageSource messageSource;

    public MobileManagementPolicyProvider(GoogleDeviceCacheService deviceCache, GoogleApiFactory directoryFactory, MessageSource messageSource) {
        this.deviceCache = deviceCache;
        this.directoryFactory = directoryFactory;
        this.messageSource = messageSource;
    }

    @Override
    public String key() {
        return "mobile_management";
    }

    @Override
    public OrgUnitPolicyDto fetch(String loggedInEmail, String orgUnitPath) throws Exception {
        MdStats stats = getOrFetchStats(loggedInEmail).getOrDefault(
                normalizePath(orgUnitPath),
                new MdStats(0, 0, 0)
        );

        int advanced = stats.advanced();
        int basic = stats.basic();
        int total = stats.total();

        Locale locale = LocaleContextHolder.getLocale();

        String status;
        String css;
        String description;
        String baseExplanation = messageSource.getMessage("mobile.management.dto.base_explanation", null, locale);

        if (total == 0) {
            status = messageSource.getMessage("mobile.management.dto.no_devices.status", null, locale);
            css = "bg-slate-100 text-slate-700";
            description = messageSource.getMessage("mobile.management.dto.no_devices.description", null, locale);
        } else if (basic == 0) {
            status = "OK (" + advanced + "/" + total + " Advanced)";
            css = "bg-green-100 text-green-800";
            description = messageSource.getMessage("mobile.management.dto.basic_zero.description", null, locale);
        } else if (advanced == 0) {
            status = "Basic (" + basic + "/" + total + ")";
            css = "bg-amber-100 text-amber-800";
            description = messageSource.getMessage("mobile.management.dto.advanced_zero.description", null, locale);
        } else {
            status = messageSource.getMessage("mobile.management.dto.mixed.status", null, locale) + " (" + advanced + " Advanced, " + basic + " Basic)";
            css = "bg-amber-100 text-amber-800";
            description = advanced + " " + messageSource.getMessage("mobile.management.dto.mixed.description", null, locale) + " Advanced, " + basic + " Basic";
        }

        String details = total > 0 ? "Advanced: " + advanced + " • Basic: " + basic + " • "+messageSource.getMessage("mobile.management.dto.mixed.details", null, locale)+": " + total : null;

        return new OrgUnitPolicyDto(
                key(),
                messageSource.getMessage("mobile.management.dto.title", null, locale),
                description,
                status,
                css,
                baseExplanation,
                null,
                false,
                "https://admin.google.com/u/1/ac/devices/settings/general",
                details
        );
    }

    public void forceRefreshCache(String adminEmail) {
        cache.remove(adminEmail);
    }

    private Map<String, MdStats> getOrFetchStats(String loggedInEmail) {
        return cache.compute(loggedInEmail, (email, existing) -> {
            long now = System.currentTimeMillis();
            if (existing != null && (now - existing.timestamp()) < CACHE_TTL_MS) {
                return existing;
            }
            return fetchAndAggregate(email, existing);
        }).statsMap();
    }

    private CacheEntry fetchAndAggregate(String loggedInEmail, CacheEntry fallback) {
        try {
            log.info("Ophalen mobiel beheerniveau per apparaat voor: {}", loggedInEmail);

            Map<String, String> emailToOu = fetchEmailToOuMap(loggedInEmail);
            List<MobileDevice> devices = deviceCache.getOrFetchDeviceData(loggedInEmail).mobileDevices();
            if (devices == null) devices = List.of();

            Map<String, MdStats> statsMap = new HashMap<>();

            for (MobileDevice d : devices) {
                String ownerEmail = getOwnerEmail(d);
                if (ownerEmail == null || ownerEmail.isBlank()) continue;

                String ouPath = emailToOu.getOrDefault(ownerEmail.toLowerCase(), "/");
                if (ouPath == null || ouPath.isBlank()) ouPath = "/";

                boolean isAdvanced = inferAdvanced(d);

                MdStats current = statsMap.getOrDefault(ouPath, new MdStats(0, 0, 0));
                statsMap.put(ouPath, new MdStats(
                        current.advanced() + (isAdvanced ? 1 : 0),
                        current.basic() + (isAdvanced ? 0 : 1),
                        current.total() + 1
                ));
            }

            return new CacheEntry(statsMap, System.currentTimeMillis());

        } catch (Exception e) {
            log.warn("Kon mobiel beheerniveau niet ophalen: {}", e.getMessage());
            if (fallback != null) return fallback;
            return new CacheEntry(new HashMap<>(), System.currentTimeMillis());
        }
    }

    private Map<String, String> fetchEmailToOuMap(String loggedInEmail) throws Exception {
        Directory directory = directoryFactory.getDirectoryService(Set.of(DIRECTORY_USER_READONLY), loggedInEmail);
        Map<String, String> map = new HashMap<>();
        String pageToken = null;

        do {
            Directory.Users.List req = directory.users().list()
                    .setCustomer("my_customer")
                    .setMaxResults(500)
                    .setFields("nextPageToken, users(primaryEmail, orgUnitPath)");
            if (pageToken != null) req.setPageToken(pageToken);

            Users users = req.execute();
            if (users.getUsers() != null) {
                for (User u : users.getUsers()) {
                    String email = u.getPrimaryEmail();
                    String path = (u.getOrgUnitPath() != null && !u.getOrgUnitPath().isBlank()) ? u.getOrgUnitPath().trim() : "/";
                    if (email != null && !email.isBlank()) {
                        map.put(email.toLowerCase(), path);
                    }
                }
            }
            pageToken = users.getNextPageToken();
        } while (pageToken != null && !pageToken.isBlank());

        return map;
    }

    private String getOwnerEmail(MobileDevice d) {
        if (d.getEmail() != null && !d.getEmail().isEmpty()) {
            return d.getEmail().get(0);
        }
        if (d.getName() != null && !d.getName().isEmpty()) {
            String first = d.getName().get(0);
            if (first != null && first.contains("@")) return first;
        }
        return null;
    }

    /**
     * Infers Advanced management from device fields.
     * Advanced devices report: encryptionStatus, devicePasswordStatus, applications (Android).
     */
    private boolean inferAdvanced(MobileDevice d) {
        if (d.getEncryptionStatus() != null && !d.getEncryptionStatus().isBlank()) return true;
        if (d.getDevicePasswordStatus() != null && !d.getDevicePasswordStatus().isBlank()) return true;
        if (d.getDeviceCompromisedStatus() != null && !d.getDeviceCompromisedStatus().isBlank()) return true;
        if (d.getApplications() != null && !d.getApplications().isEmpty()) return true;
        return false;
    }

    private static String normalizePath(String path) {
        return (path == null || path.isBlank()) ? "/" : path.trim();
    }

    private record CacheEntry(Map<String, MdStats> statsMap, long timestamp) {}
    private record MdStats(int advanced, int basic, int total) {}
}
