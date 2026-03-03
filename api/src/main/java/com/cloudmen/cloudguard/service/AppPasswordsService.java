package com.cloudmen.cloudguard.service;

import com.cloudmen.cloudguard.dto.passwords.AppPasswordCacheEntry;
import com.cloudmen.cloudguard.dto.passwords.AppPasswordDto;
import com.cloudmen.cloudguard.dto.passwords.AppPasswordOverviewResponse;
import com.cloudmen.cloudguard.dto.passwords.AppPasswordPageResponse;
import com.cloudmen.cloudguard.dto.passwords.UserAppPasswordsDto;
import com.cloudmen.cloudguard.utility.GoogleApiFactory;
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
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.concurrent.TimeUnit;

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

    public AppPasswordsService(GoogleApiFactory apiFactory) {
        this.apiFactory = apiFactory;
    }

    public AppPasswordPageResponse getAppPasswordsPaged(String adminEmail, String pageToken, int size, String query) {
        AppPasswordCacheEntry entry = cache.get(adminEmail, this::fetchAllAppPasswords);
        List<UserAppPasswordsDto> allUsers = entry.users();

        if (query != null && !query.trim().isEmpty()) {
            String lowerQuery = query.toLowerCase().trim();
            allUsers = allUsers.stream()
                    .filter(u -> (u.name() != null && u.name().toLowerCase().contains(lowerQuery))
                            || (u.email() != null && u.email().toLowerCase().contains(lowerQuery)))
                    .toList();
        }

        int page = 1;
        if (pageToken != null && !pageToken.isBlank()) {
            try {
                int parsed = Integer.parseInt(pageToken);
                if (parsed > 0) page = parsed;
            } catch (NumberFormatException ignored) {
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

    public AppPasswordOverviewResponse getOverview(String adminEmail) {
        AppPasswordCacheEntry entry = cache.get(adminEmail, this::fetchAllAppPasswords);
        int usersWithAppPasswords = entry.users().size();
        int totalAppPasswords = entry.users().stream()
                .mapToInt(u -> u.passwords().size())
                .sum();
        int totalUserCount = entry.totalUserCount();
        int securityScore = totalUserCount == 0 ? 100
                : (int) Math.round(100.0 * (totalUserCount - usersWithAppPasswords) / totalUserCount);
        return new AppPasswordOverviewResponse(true, totalAppPasswords, totalAppPasswords, securityScore);
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

    private AppPasswordDto mapToDto(Asp asp) {
        AppPasswordDto dto = new AppPasswordDto();
        dto.setCodeId(asp.getCodeId());
        dto.setName(asp.getName());
        dto.setCreationTime(asp.getCreationTime() != null ? String.valueOf(asp.getCreationTime()) : null);
        dto.setLastTimeUsed(asp.getLastTimeUsed() != null ? String.valueOf(asp.getLastTimeUsed()) : null);
        return dto;
    }
}
