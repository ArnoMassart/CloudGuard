package com.cloudmen.cloudguard.service;

import com.cloudmen.cloudguard.dto.apppasswords.*;
import com.cloudmen.cloudguard.dto.password.SecurityScoreBreakdownDto;
import com.cloudmen.cloudguard.dto.password.SecurityScoreFactorDto;
import com.cloudmen.cloudguard.service.preference.SecurityPreferenceScoreSupport;
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

import static com.cloudmen.cloudguard.utility.GoogleServiceHelperMethods.severity;

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

    public AppPasswordsService(GoogleApiFactory apiFactory, MessageSource messageSource) {
        this.apiFactory = apiFactory;
        this.messageSource = messageSource;
    }

    public AppPasswordPageResponse getAppPasswordsPaged(String adminEmail, String pageToken, int size, String query, boolean isTestMode) {
        AppPasswordCacheEntry entry = isTestMode ? createMockAppPasswords() : cache.get(adminEmail, this::fetchAllAppPasswords);
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

    public AppPasswordOverviewResponse getOverview(String adminEmail, boolean isTestMode) {
        return getOverview(adminEmail, isTestMode, Set.of());
    }

    public AppPasswordOverviewResponse getOverview(String adminEmail, boolean isTestMode, Set<String> disabledKeys) {
        Set<String> off = disabledKeys == null ? Set.of() : disabledKeys;
        AppPasswordCacheEntry entry = isTestMode ? createMockAppPasswords() : cache.get(adminEmail, this::fetchAllAppPasswords);
        int usersWithAppPasswords = entry.users().size();
        int totalAppPasswords = entry.users().stream()
                .mapToInt(u -> u.passwords().size())
                .sum();
        int totalUserCount = entry.totalUserCount();
        boolean ignored = SecurityPreferenceScoreSupport.preferenceDisabled(off, "app-passwords", "appPassword");
        int securityScore = ignored ? 100 : totalUserCount == 0 ? 100
                : (int) Math.round(100.0 * (totalUserCount - usersWithAppPasswords) / totalUserCount);
        SecurityScoreBreakdownDto breakdown = buildAppPasswordsBreakdown(
                totalUserCount, usersWithAppPasswords, totalAppPasswords, securityScore, ignored);
        return new AppPasswordOverviewResponse(true, totalAppPasswords, usersWithAppPasswords, securityScore, breakdown);
    }

    private SecurityScoreBreakdownDto buildAppPasswordsBreakdown(int totalUserCount, int usersWithAppPasswords, int totalAppPasswords, int securityScore,
                                                                 boolean ignorePref) {
        int usersWithoutScore = ignorePref ? 100 : totalUserCount == 0 ? 100
                : (int) Math.round(100.0 * (totalUserCount - usersWithAppPasswords) / totalUserCount);
        int totalCountScore = ignorePref ? 100 : totalAppPasswords == 0 ? 100 : (int) Math.max(0, 100 - Math.min(totalAppPasswords, 50) * 2);

        Locale locale = LocaleContextHolder.getLocale();

        var factors = java.util.List.of(
                new SecurityScoreFactorDto(messageSource.getMessage("app-passwords.score.factor.users_without.title", null, locale),
                        usersWithAppPasswords == 0
                                ? messageSource.getMessage("app-passwords.score.factor.users_without.description.none", null, locale)
                                : messageSource.getMessage("app-passwords.score.factor.users_without.description", new Object[]{usersWithAppPasswords, totalUserCount}, locale),
                        usersWithoutScore, 100, severity(usersWithoutScore)),
                new SecurityScoreFactorDto(messageSource.getMessage("app-passwords.score.factor.total.title", null, locale),
                        totalAppPasswords == 0
                                ? messageSource.getMessage("app-passwords.score.factor.total.description.none", null, locale)
                                : messageSource.getMessage("app-passwords.score.factor.total.description", new Object[]{totalAppPasswords}, locale),
                        totalCountScore, 100, severity(totalCountScore, true))
        );
        String status = securityScore == 100 ? "perfect" : securityScore >= 75 ? "good" : securityScore > 50 ? "average" : "bad";
        return new SecurityScoreBreakdownDto(securityScore, status, factors);
    }

    private static String severity(double score) {
        if (score >= 75) return "success";
        if (score >= 50) return "warning";
        return "error";
    }

    public void forceRefreshCache(String adminEmail) {
        cache.asMap().compute(adminEmail, (email, existing) -> fetchAllAppPasswords(email));
    }

    private AppPasswordCacheEntry fetchAllAppPasswords(String adminEmail) {
        try {
            log.info("Ophalen LIVE app passwords van Google voor: {}", adminEmail);
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
                                ? u.getName().getFullName() : userEmail;
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
            throw new RuntimeException("Failed to fetch app passwords", e);
        }
    }

    private AppPasswordCacheEntry createMockAppPasswords() {
        long now = System.currentTimeMillis();
        long dayMs = 24L * 60 * 60 * 1000;

        AppPasswordDto p1 = new AppPasswordDto(101, "Outlook Desktop", String.valueOf(now - 350 * dayMs), DateTimeConverter.convertToTimeAgo(now - 27 * dayMs));
        AppPasswordDto p2 = new AppPasswordDto(102, "Thunderbird", String.valueOf(now - 200 * dayMs), DateTimeConverter.convertToTimeAgo(now - 3 * dayMs));
        AppPasswordDto p3a = new AppPasswordDto(103, "Apple Mail", String.valueOf(now - 180 * dayMs), DateTimeConverter.convertToTimeAgo(now - dayMs));
        AppPasswordDto p3b = new AppPasswordDto(104, "Outlook Desktop", String.valueOf(now - 90 * dayMs), null);
        AppPasswordDto p4 = new AppPasswordDto(105, "Google Calendar Sync", String.valueOf(now - 60 * dayMs), DateTimeConverter.convertToTimeAgo(now - 14 * dayMs));
        AppPasswordDto p5a = new AppPasswordDto(106, "Thunderbird", String.valueOf(now - 400 * dayMs), DateTimeConverter.convertToTimeAgo(now - 45 * dayMs));
        AppPasswordDto p5b = new AppPasswordDto(107, "Outlook Mobile", String.valueOf(now - 120 * dayMs), DateTimeConverter.convertToTimeAgo(now));

        List<UserAppPasswordsDto> users = List.of(
                new UserAppPasswordsDto("demo-1", "Pieter de Vries", "pieter.devries@bedrijf.nl", "User", false, List.of(p1)),
                new UserAppPasswordsDto("demo-2", "Thomas Mulder", "thomas.mulder@bedrijf.nl", "User", true, List.of(p2)),
                new UserAppPasswordsDto("demo-3", "Lisa van Berg", "lisa.vanberg@bedrijf.nl", "Admin", true, List.of(p3a, p3b)),
                new UserAppPasswordsDto("demo-4", "Jan Bakker", "jan.bakker@bedrijf.nl", "User", true, List.of(p4)),
                new UserAppPasswordsDto("demo-5", "Sophie Jansen", "sophie.jansen@bedrijf.nl", "User", false, List.of(p5a, p5b))
        );
        return new AppPasswordCacheEntry(users, 15);
    }

    private AppPasswordDto mapToDto(Asp asp) {
        AppPasswordDto dto = new AppPasswordDto();
        dto.setCodeId(asp.getCodeId());
        dto.setName(asp.getName());
        dto.setCreationTime(asp.getCreationTime() != null ? String.valueOf(asp.getCreationTime()) : null);
        dto.setLastTimeUsed(asp.getLastTimeUsed() != null ? String.valueOf(asp.getLastTimeUsed()) : null);
        return dto;
    }
}
