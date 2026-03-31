package com.cloudmen.cloudguard.service;

import com.cloudmen.cloudguard.dto.adminsecuritykeys.AdminSecurityKeysResponse;
import com.cloudmen.cloudguard.dto.adminsecuritykeys.AdminWithSecurityKeyDto;
import com.cloudmen.cloudguard.exception.GoogleWorkspaceSyncException;
import com.cloudmen.cloudguard.utility.GoogleApiFactory;
import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.api.services.admin.directory.Directory;
import com.google.api.services.admin.directory.DirectoryScopes;
import com.google.api.services.admin.directory.model.User;
import com.google.api.services.admin.directory.model.Users;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.MessageSource;
import org.springframework.context.i18n.LocaleContextHolder;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;

import java.net.URI;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.concurrent.TimeUnit;

@Service
public class AdminSecurityKeysService {

    private static final Logger log = LoggerFactory.getLogger(AdminSecurityKeysService.class);
    private static final String REPORTS_API_SCOPE = "https://www.googleapis.com/auth/admin.reports.usage.readonly";
    private static final String REPORTS_API_BASE = "https://admin.googleapis.com/admin/reports/v1/usage/users/all/dates";
    private static final String PARAMETERS = "accounts:num_security_keys,accounts:num_passkeys_enrolled,accounts:first_name,accounts:last_name,accounts:is_2sv_enrolled";

    private final GoogleApiFactory apiFactory;
    private final MessageSource messageSource;
    private final RestTemplate restTemplate = new RestTemplate();
    private final ObjectMapper mapper = new ObjectMapper();

    private final Cache<String, AdminSecurityKeysResponse> cache = Caffeine.newBuilder()
            .expireAfterWrite(1, TimeUnit.HOURS)
            .maximumSize(100)
            .build();

    public AdminSecurityKeysService(GoogleApiFactory apiFactory, @Qualifier("messageSource") MessageSource messageSource) {
        this.apiFactory = apiFactory;
        this.messageSource = messageSource;
    }

    public AdminSecurityKeysResponse getAdminsWithSecurityKeys(String adminEmail) {
        return cache.get(adminEmail, this::fetchAdminsWithoutSecurityKeys);
    }

    public void forceRefreshCache(String adminEmail) {
        cache.asMap().compute(adminEmail, (email, existing) -> fetchAdminsWithoutSecurityKeys(email));
    }

    private AdminSecurityKeysResponse fetchAdminsWithoutSecurityKeys(String adminEmail) {
        try {
            Set<String> directoryScopes = Set.of(
                    DirectoryScopes.ADMIN_DIRECTORY_USER_READONLY
            );
            Directory directory = apiFactory.getDirectoryService(directoryScopes, adminEmail);

            // 1. Fetch all admin users from Directory API
            List<User> adminUsers = fetchAdminUsers(directory);
            if (adminUsers.isEmpty()) {
                return new AdminSecurityKeysResponse(Collections.emptyList(), 0);
            }

            Set<String> adminEmails = new HashSet<>();
            Map<String, User> emailToUser = new HashMap<>();
            for (User u : adminUsers) {
                String email = u.getPrimaryEmail();
                if (email != null && !email.isBlank()) {
                    adminEmails.add(email.toLowerCase());
                    emailToUser.put(email.toLowerCase(), u);
                }
            }

            // 2. Fetch usage report (num_security_keys, num_passkeys_enrolled) for all users
            Map<String, SecurityKeyCounts> emailToCounts = fetchSecurityKeyCounts(adminEmail);

            // 3. Filter: keep only admins WITHOUT hardware security keys
            List<AdminWithSecurityKeyDto> result = new ArrayList<>();
            for (String email : adminEmails) {
                SecurityKeyCounts counts = emailToCounts.get(email);
                if (counts != null && counts.numSecurityKeys > 0) continue;

                User user = emailToUser.get(email);
                if (user == null) continue;

                String id = user.getId() != null ? user.getId() : email;
                String name = user.getName() != null && user.getName().getFullName() != null
                        ? user.getName().getFullName() : email;
                String orgUnitPath = user.getOrgUnitPath() != null ? user.getOrgUnitPath() : "/";
                boolean twoFactorEnabled = Boolean.TRUE.equals(user.getIsEnrolledIn2Sv());

                int numSecurityKeys = counts != null ? counts.numSecurityKeys : 0;
                result.add(new AdminWithSecurityKeyDto(
                        id,
                        name,
                        user.getPrimaryEmail(),
                        "Admin",
                        orgUnitPath,
                        twoFactorEnabled,
                        numSecurityKeys
                ));
            }

            result.sort(Comparator.comparing(AdminWithSecurityKeyDto::name, String.CASE_INSENSITIVE_ORDER));
            return new AdminSecurityKeysResponse(result, adminUsers.size());

        } catch (Exception e) {
            String errMsg = e.getMessage();
            boolean isAuthError = errMsg != null && (errMsg.contains("401 Unauthorized") || errMsg.contains("Error getting access token"));
            if (isAuthError) {
                String userMsg = "Reports API scope niet geautoriseerd. Voeg https://www.googleapis.com/auth/admin.reports.usage.readonly toe aan domain-wide delegation in Google Admin Console (Security → API Controls → Domain-wide delegation).";
                log.warn("{} Cause: {}", userMsg, errMsg);
                return new AdminSecurityKeysResponse(Collections.emptyList(), 0, userMsg);
            }
            log.error("Failed to fetch admins without security keys", e);
            throw new GoogleWorkspaceSyncException(
                    messageSource.getMessage(
                            "api.google.admin_security_keys_fetch_failed", null, LocaleContextHolder.getLocale()),
                    e);
        }
    }

    private List<User> fetchAdminUsers(Directory directory) throws Exception {
        List<User> admins = new ArrayList<>();
        String pageToken = null;

        do {
            Directory.Users.List req = directory.users()
                    .list()
                    .setCustomer("my_customer")
                    .setMaxResults(500)
                    .setQuery("isAdmin=true")
                    .setFields("nextPageToken, users(id, primaryEmail, name/fullName, orgUnitPath, isEnrolledIn2Sv)");

            if (pageToken != null) req.setPageToken(pageToken);
            Users users = req.execute();

            if (users.getUsers() != null) {
                admins.addAll(users.getUsers());
            }
            pageToken = users.getNextPageToken();
        } while (pageToken != null && !pageToken.isEmpty());

        log.info("Fetched {} admin users from Directory API", admins.size());
        return admins;
    }

    private Map<String, SecurityKeyCounts> fetchSecurityKeyCounts(String adminEmail) throws Exception {
        var creds = apiFactory.getCredentials(Set.of(REPORTS_API_SCOPE), adminEmail);
        creds.refreshIfExpired();
        String token = creds.getAccessToken().getTokenValue();

        Map<String, SecurityKeyCounts> result = new HashMap<>();

        // We will try up to 7 days back.
        int maxDaysBack = 7;

        for (int daysBack = 3; daysBack <= maxDaysBack; daysBack++) {
            String date = LocalDate.now().minusDays(daysBack).format(DateTimeFormatter.ISO_LOCAL_DATE);
            String pageToken = null;
            boolean dataNotYetAvailable = false;

            log.info("Attempting to fetch Reports API data for date: {}", date);

            try {
                do {
                    UriComponentsBuilder builder = UriComponentsBuilder
                            .fromHttpUrl(REPORTS_API_BASE + "/" + date)
                            .queryParam("parameters", PARAMETERS)
                            .queryParam("maxResults", 500);
                    if (pageToken != null) builder.queryParam("pageToken", pageToken);

                    URI uri = builder.build().encode().toUri();
                    HttpHeaders headers = new HttpHeaders();
                    headers.setBearerAuth(token);

                    ResponseEntity<String> resp;
                    try {
                        resp = restTemplate.exchange(
                                uri, HttpMethod.GET, new HttpEntity<>(headers), String.class);
                    } catch (org.springframework.web.client.HttpClientErrorException e) {
                        // Check if the error is exactly the 400 "not yet available" lag issue
                        if (e.getStatusCode().value() == 400 && e.getResponseBodyAsString().contains("not yet available")) {
                            log.warn("Google Reports API data not yet available for {}, falling back to older date...", date);
                            dataNotYetAvailable = true;
                            break; // Break the pagination do-while loop, let the for loop try the next day
                        }
                        throw e; // Rethrow if it's a different HTTP error (e.g., 401 Unauthorized or a different 400)
                    }

                    if (resp.getBody() == null) break;

                    JsonNode root = mapper.readTree(resp.getBody());
                    JsonNode usageReports = root.get("usageReports");
                    if (usageReports == null || !usageReports.isArray()) break;

                    for (JsonNode report : usageReports) {
                        JsonNode entity = report.get("entity");
                        if (entity == null) continue;
                        String userEmail = entity.has("userEmail") ? entity.get("userEmail").asText() : null;
                        if (userEmail == null || userEmail.isBlank()) continue;

                        int numSecurityKeys = 0;
                        int numPasskeysEnrolled = 0;

                        JsonNode params = report.get("parameters");
                        if (params != null && params.isArray()) {
                            for (JsonNode p : params) {
                                String name = p.has("name") ? p.get("name").asText() : "";
                                if ("accounts:num_security_keys".equals(name) && p.has("intValue")) {
                                    numSecurityKeys = p.get("intValue").asInt();
                                } else if ("accounts:num_passkeys_enrolled".equals(name) && p.has("intValue")) {
                                    numPasskeysEnrolled = p.get("intValue").asInt();
                                }
                            }
                        }

                        result.put(userEmail.toLowerCase(), new SecurityKeyCounts(numSecurityKeys, numPasskeysEnrolled));
                    }

                    JsonNode next = root.get("nextPageToken");
                    pageToken = (next != null && !next.isNull() && !next.asText().isBlank()) ? next.asText() : null;
                } while (pageToken != null);

                // If we successfully paginated through the whole date without hitting the "not available" flag:
                if (!dataNotYetAvailable) {
                    log.info("Successfully fetched security key counts for {} users from Reports API for date {}", result.size(), date);
                    return result; // Exit the loop and return the data!
                }

            } catch (Exception e) {
                if (daysBack == maxDaysBack) {
                    throw new Exception("Failed to fetch from Reports API even after " + maxDaysBack + " days fallback.", e);
                }
                log.warn("Unexpected error fetching date {}, trying older date. Error: {}", date, e.getMessage());
            }
        }

        return result;
    }

    private record SecurityKeyCounts(int numSecurityKeys, int numPasskeysEnrolled) {}
}
