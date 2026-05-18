package com.cloudmen.cloudguard.service;

import com.cloudmen.cloudguard.exception.GoogleWorkspaceSyncException;
import com.cloudmen.cloudguard.dto.apppasswords.*;
import com.cloudmen.cloudguard.dto.password.SecurityScoreBreakdownDto;
import com.cloudmen.cloudguard.service.preference.SecurityPreferenceScoreSupport;
import com.cloudmen.cloudguard.service.user.UserService;
import com.cloudmen.cloudguard.utility.DateTimeConverter;
import com.cloudmen.cloudguard.utility.GoogleApiFactory;
import com.cloudmen.cloudguard.utility.GoogleServiceHelperMethods;
import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import com.google.api.services.admin.directory.Directory;
import com.google.api.services.admin.directory.DirectoryScopes;
import com.google.api.services.admin.directory.model.Asp;
import com.google.api.services.admin.directory.model.Asps;
import com.google.api.services.admin.directory.model.User;
import com.google.api.services.admin.directory.model.Users;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.MessageSource;
import org.springframework.context.i18n.LocaleContextHolder;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.concurrent.TimeUnit;

import static com.cloudmen.cloudguard.utility.GoogleServiceHelperMethods.*;

/**
 * Loads Google Workspace <strong>application-specific passwords</strong> (ASPs) via Admin SDK {@code asps.list} for each
 * user under {@code my_customer}, caches the flattened result per logged-in viewer for one hour, and exposes paging plus
 * an overview score based on the share of users <em>without</em> any ASP (higher is better).
 * <p>
 * Only users with at least one ASP appear in list payloads; {@link AppPasswordCacheEntry#totalUserCount()} counts every
 * user scanned for the denominator used in {@link #getOverview(String, Set)}.
 *
 * @see com.cloudmen.cloudguard.controller.AppPasswordController
 */
@Service
public class AppPasswordsService {

    private static final Logger log = LoggerFactory.getLogger(AppPasswordsService.class);

    private final GoogleApiFactory apiFactory;
    private final Cache<String, AppPasswordCacheEntry> cache = Caffeine.newBuilder()
            .expireAfterWrite(1, TimeUnit.HOURS)
            .maximumSize(100)
            .build();

    private final Set<String> scopes = Set.of(
            DirectoryScopes.ADMIN_DIRECTORY_USER_SECURITY,
            DirectoryScopes.ADMIN_DIRECTORY_USER_READONLY
    );
    private final MessageSource messageSource;
    private final UserService userService;
    private final OrganizationService organizationService;

    /**
     * @param apiFactory              Directory client factory with domain-wide delegation
     * @param messageSource           {@code app-passwords.score.factor.*} strings
     * @param userService             resolves workspace admin email for {@code loggedInEmail}
     * @param organizationService     tenant context for admin resolution
     */
    public AppPasswordsService(
            GoogleApiFactory apiFactory,
            MessageSource messageSource,
            UserService userService,
            OrganizationService organizationService) {
        this.apiFactory = apiFactory;
        this.messageSource = messageSource;
        this.userService = userService;
        this.organizationService = organizationService;
    }

    /**
     * One page of {@link UserAppPasswordsDto} rows (users with ≥1 ASP only), filtered by optional query on name/email.
     *
     * @param loggedInEmail viewer used as cache key (tenant fetch impersonates resolved admin)
     * @param pageToken     1-based page as string; invalid values fall back to page 1
     * @param size          page size, bounded to {@code [1, 100]}
     */
    public AppPasswordPageResponse getAppPasswordsPaged(String loggedInEmail, String pageToken, int size, String query) {
        AppPasswordCacheEntry entry = cache.get(loggedInEmail, this::fetchAllAppPasswords);
        List<UserAppPasswordsDto> allUsers = GoogleServiceHelperMethods.filterByNameOrEmail(
                entry.users(),
                query,
                UserAppPasswordsDto::name,
                UserAppPasswordsDto::email
        );

        int page = 1;
        if (pageToken != null && !pageToken.isBlank()) {
            try {
                int parsed = Integer.parseInt(pageToken);
                if (parsed > 0) page = parsed;
            } catch (NumberFormatException ignored) {
                log.warn("Invalid pageToken provided: '{}'. Defaulting to page 1.", pageToken);
            }
        }
        int pageSize = Math.max(1, Math.min(size, 100));

        int totalUsers = allUsers.size();
        int startIndex = (page - 1) * pageSize;
        int endIndex = Math.min(startIndex + pageSize, totalUsers);

        List<UserAppPasswordsDto> pagedUsers = (startIndex >= totalUsers)
                ? Collections.emptyList()
                : new ArrayList<>(allUsers.subList(startIndex, endIndex));

        String nextToken = (endIndex < totalUsers) ? String.valueOf(page + 1) : null;
        return new AppPasswordPageResponse(pagedUsers, nextToken);
    }

    /**
     * Overview without org preference context (delegates to {@link #getOverview(String, Set)} with an empty set).
     */
    public AppPasswordOverviewResponse getOverview(String loggedInEmail) {
        return getOverview(loggedInEmail, Set.of());
    }

    /**
     * Totals ASP inventory, counts users that still have ASPs, and computes security score
     * {@code round(100 * (totalUsers - usersWithAsp) / totalUsers)} when {@code totalUsers > 0}.
     * <p>
     * {@code disabledKeys} supplies keys evaluated by {@link SecurityPreferenceScoreSupport#preferenceDisabled(java.util.Set, String, String)}
     * ({@code app-passwords} / {@code appPassword}); the result is stored in {@code ignored} but does not yet change the score or breakdown.
     *
     * @param loggedInEmail cache key / viewer email
     * @param disabledKeys  org disabled preference keys ({@code section:key}); reserved for future score suppression
     */
    @SuppressWarnings("unused")
    public AppPasswordOverviewResponse getOverview(String loggedInEmail, Set<String> disabledKeys) {
        Set<String> off = disabledKeys == null ? Set.of() : disabledKeys;

        AppPasswordCacheEntry entry = cache.get(loggedInEmail, this::fetchAllAppPasswords);
        if (entry == null) {
            throw new GoogleWorkspaceSyncException("Error with app passwords cache, cache is null");
        }
        int usersWithAppPasswords = entry.users().size();
        int totalAppPasswords = entry.users().stream().mapToInt(u -> u.passwords().size()).sum();
        int totalUserCount = entry.totalUserCount();

        if (totalUserCount == 0) {
            return new AppPasswordOverviewResponse(true, totalAppPasswords, usersWithAppPasswords, null, null);
        }

        boolean ignored = SecurityPreferenceScoreSupport.preferenceDisabled(off, "app-passwords", "appPassword");
        int securityScore = (int) Math.round(100.0 * (totalUserCount - usersWithAppPasswords) / totalUserCount);
        SecurityScoreBreakdownDto breakdown = buildAppPasswordsBreakdown(totalUserCount, usersWithAppPasswords, securityScore);

        return new AppPasswordOverviewResponse(true, totalAppPasswords, usersWithAppPasswords, securityScore, breakdown);
    }

    /** Single-factor breakdown: users without ASPs versus workspace user inventory. */
    private SecurityScoreBreakdownDto buildAppPasswordsBreakdown(int totalUserCount, int usersWithAppPasswords, int securityScore) {
        int usersWithoutScore = totalUserCount == 0 ? 100
                : (int) Math.round(100.0 * (totalUserCount - usersWithAppPasswords) / totalUserCount);

        Locale locale = LocaleContextHolder.getLocale();

        boolean showFactors = totalUserCount > 0;

        var factors = java.util.List.of(
                securityScoreFactorForDetail(
                        showFactors,
                        messageSource.getMessage("app-passwords.score.factor.users_without.title", null, locale),
                        usersWithAppPasswords == 0
                                ? messageSource.getMessage("app-passwords.score.factor.users_without.description.none", null, locale)
                                : messageSource.getMessage(
                                        "app-passwords.score.factor.users_without.description",
                                        new Object[] {usersWithAppPasswords, totalUserCount},
                                        locale),
                        usersWithoutScore,
                        100,
                        severity(usersWithoutScore)));
        String status = getOverviewStatus(securityScore);
        return new SecurityScoreBreakdownDto(securityScore, status, factors);
    }

    /** Recomputes and stores cache entry for {@code loggedInEmail}. */
    public void forceRefreshCache(String loggedInEmail) {
        cache.asMap().compute(loggedInEmail, (email, existing) -> fetchAllAppPasswords(email));
    }

    /**
     * Pages all workspace users, calls {@code asps.list} per user, and builds {@link AppPasswordCacheEntry} containing only
     * users with passwords plus {@code totalUserCount} of users enumerated.
     */
    private AppPasswordCacheEntry fetchAllAppPasswords(String loggedInEmail) {
        try {
            String adminEmail = GoogleServiceHelperMethods.getAdminEmailForUser(loggedInEmail, userService, organizationService);

            log.info("Ophalen LIVE App Passwords van Google. Gebruiker: {}, Impersonatie via Admin: {}", loggedInEmail, adminEmail);
            Directory directory = apiFactory.getDirectoryService(scopes, adminEmail);

            List<UserAppPasswordsDto> result = new ArrayList<>();
            int totalUserCount = 0;
            String pageToken = null;

            do {
                Directory.Users.List req = directory.users()
                        .list()
                        .setCustomer("my_customer")
                        .setMaxResults(100)
                        .setFields("nextPageToken, users(id, primaryEmail, name/fullName, isAdmin, isEnrolledIn2Sv)");

                if (pageToken != null) req.setPageToken(pageToken);
                Users users = req.execute();

                if (users.getUsers() != null) {
                    for (User u : users.getUsers()) {
                        totalUserCount++;
                        String userId = u.getId() != null ? u.getId() : u.getPrimaryEmail();
                        String userEmail = u.getPrimaryEmail();
                        String userName = u.getName() != null && u.getName().getFullName() != null
                                ? u.getName().getFullName()
                                : userEmail;
                        String role = Boolean.TRUE.equals(u.getIsAdmin()) ? "Admin" : "User";
                        boolean twoFactorEnabled = Boolean.TRUE.equals(u.getIsEnrolledIn2Sv());

                        List<AppPasswordDto> passwords = Collections.emptyList();
                        try {
                            Asps asps = directory.asps().list(userEmail).execute();
                            if (asps.getItems() != null && !asps.getItems().isEmpty()) {
                                passwords = asps.getItems().stream().map(this::mapToDto).toList();
                            }
                        } catch (Exception e) {
                            log.warn("Kon app-wachtwoorden niet ophalen voor {}: {}", userEmail, e.getMessage());
                        }

                        if (passwords.isEmpty()) continue;
                        result.add(new UserAppPasswordsDto(userId, userName, userEmail, role, twoFactorEnabled, passwords));
                    }
                }
                pageToken = users.getNextPageToken();

            } while (pageToken != null && !pageToken.isEmpty());

            return new AppPasswordCacheEntry(result, totalUserCount);

        } catch (Exception e) {
            throw new GoogleWorkspaceSyncException("Fout bij ophalen app-wachtwoorden: " + e.getMessage());
        }
    }

    /** Maps Directory {@link Asp} into UI-facing dates ({@link DateTimeConverter}). */
    private AppPasswordDto mapToDto(Asp asp) {
        AppPasswordDto dto = new AppPasswordDto();
        dto.setCodeId(asp.getCodeId());
        dto.setName(asp.getName());
        dto.setCreationTime(
                asp.getCreationTime() != null ? DateTimeConverter.parseWithPattern(asp.getCreationTime(), "dd-MM-yyyy") : null);
        dto.setLastTimeUsed(
                asp.getLastTimeUsed() != null ? DateTimeConverter.convertToTimeAgo(asp.getLastTimeUsed()) : null);
        return dto;
    }
}
