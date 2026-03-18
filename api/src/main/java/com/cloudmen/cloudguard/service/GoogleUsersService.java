package com.cloudmen.cloudguard.service;

import com.cloudmen.cloudguard.dto.password.SecurityScoreBreakdownDto;
import com.cloudmen.cloudguard.dto.password.SecurityScoreFactorDto;
import com.cloudmen.cloudguard.dto.users.*;
import com.cloudmen.cloudguard.service.cache.GoogleUsersCacheService;
import com.cloudmen.cloudguard.utility.DateTimeConverter;
import com.cloudmen.cloudguard.utility.GoogleServiceHelperMethods;
import com.google.api.services.admin.directory.model.User;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

@Service
public class GoogleUsersService {
    private final GoogleUsersCacheService usersCacheService;

    public GoogleUsersService(GoogleUsersCacheService usersCacheService) {
        this.usersCacheService = usersCacheService;
    }

    public void forceRefreshCache(String loggedInEmail) {
        usersCacheService.forceRefreshCache(loggedInEmail);
    }

    public UserPageResponse getWorkspaceUsersPaged(String loggedInEmail, String pageToken, int size, String query) {
        UserCacheEntry cachedData = usersCacheService.getOrFetchUsersData(loggedInEmail);

        List<User> filteredList = cachedData.allUsers();
        if (query != null && !query.trim().isEmpty()) {
            String lowerQuery = query.toLowerCase().trim();
            filteredList = filteredList.stream()
                    .filter(u -> (u.getPrimaryEmail() != null && u.getPrimaryEmail().toLowerCase().contains(lowerQuery)) ||
                            (u.getName() != null && u.getName().getFullName().toLowerCase().contains(lowerQuery)))
                    .toList();
        }

        List<User> sortedList = filteredList.stream().sorted(Comparator.comparing(a -> a.getName().getFullName())).toList();

        int page = GoogleServiceHelperMethods.getPage(pageToken);

        int totalUsers = sortedList.size();
        int startIndex = (page - 1) * size;
        int endIndex = Math.min(startIndex + size, totalUsers);

        List<User> pagedGoogleUsers = (startIndex >= totalUsers) ? Collections.emptyList() : sortedList.subList(startIndex, endIndex);

        List<UserOrgDetail> mappedUsers = pagedGoogleUsers.stream().map(user -> {
            Long roleId = cachedData.userRoleAssignments().get(user.getId());
            cachedData.userRoleAssignments().forEach((d, i) -> {
            });
            String roleName = (roleId != null) ? cachedData.roleDictionary().getOrDefault(roleId, "Unknown Role") : "Regular User";

            boolean isActive = !Boolean.TRUE.equals(user.getSuspended());
            boolean twoFAEnabled = Boolean.TRUE.equals(user.getIsEnrolledIn2Sv());

            return new UserOrgDetail(
                    user.getName().getFullName(),
                    user.getPrimaryEmail(),
                    GoogleServiceHelperMethods.translateRoleName(roleName),
                    isActive,
                    user.getLastLoginTime() != null ? DateTimeConverter.convertToTimeAgo(user.getLastLoginTime()) : "Nooit",
                    twoFAEnabled,
                    GoogleServiceHelperMethods.checkUserSecurityStatus(isActive, user.getLastLoginTime(), twoFAEnabled)
            );
        }).toList();

        String nextTokenToReturn = (endIndex < totalUsers) ? String.valueOf(page + 1) : null;
        return new UserPageResponse(mappedUsers, nextTokenToReturn);
    }

//for notifications
    public UsersWithoutTwoFactorResponse getUsersWithoutTwoFactor(String loggedInEmail) {
        UserCacheEntry cachedData = usersCacheService.getOrFetchUsersData(loggedInEmail);
        List<UsersWithoutTwoFactorResponse.UserSummary> users = cachedData.allUsers().stream()
                .filter(user -> !Boolean.TRUE.equals(user.getSuspended()) && !Boolean.TRUE.equals(user.getIsEnrolledIn2Sv()))
                .map(user -> new UsersWithoutTwoFactorResponse.UserSummary(
                        user.getName() != null && user.getName().getFullName() != null ? user.getName().getFullName() : "",
                        user.getPrimaryEmail() != null ? user.getPrimaryEmail() : ""))
                .toList();
        return new UsersWithoutTwoFactorResponse(users);
    }

    public UserOverviewResponse getUsersPageOverview(String loggedInEmail) {
        UserCacheEntry cachedData = usersCacheService.getOrFetchUsersData(loggedInEmail);
        List<User> googleUsers = cachedData.allUsers();
        LocalDate now = LocalDate.now();

        long totalUsers = googleUsers.size();
        long withoutTwoFactor = googleUsers.stream()
                .filter(user -> !Boolean.TRUE.equals(user.getSuspended()) && !Boolean.TRUE.equals(user.getIsEnrolledIn2Sv()))
                .count();
        long adminUsers = googleUsers.stream().filter(User::getIsAdmin).count();
        long securityScore = calculateSecurityScore(googleUsers);

        long activeLongNoLoginCount = googleUsers.stream().filter(user -> {
            if (user.getLastLoginTime() == null) return false;
            LocalDate loginDate = DateTimeConverter.convertGoogleDateTime(user.getLastLoginTime());
            return !Boolean.TRUE.equals(user.getSuspended()) && ChronoUnit.DAYS.between(loginDate, now) >= 90;
        }).count();

        long inactiveRecentLoginCount = googleUsers.stream().filter(user -> {
            if (user.getLastLoginTime() == null) return false;
            LocalDate loginDate = DateTimeConverter.convertGoogleDateTime(user.getLastLoginTime());
            return Boolean.TRUE.equals(user.getSuspended()) && ChronoUnit.DAYS.between(loginDate, now) <= 7;
        }).count();

        SecurityScoreBreakdownDto breakdown = buildUsersBreakdown(
                (int) totalUsers, (int) withoutTwoFactor, (int) activeLongNoLoginCount, (int) inactiveRecentLoginCount, (int) securityScore);

        return new UserOverviewResponse((int) totalUsers, (int) withoutTwoFactor, (int) adminUsers, (int) securityScore, (int) activeLongNoLoginCount, (int) inactiveRecentLoginCount, breakdown);
    }

    private SecurityScoreBreakdownDto buildUsersBreakdown(int totalUsers, int withoutTwoFactor, int activeLongNoLoginCount, int inactiveRecentLoginCount, int securityScore) {
        int twoFaScore = totalUsers == 0 ? 100 : (int) Math.round((totalUsers - withoutTwoFactor) * 100.0 / totalUsers);
        int activeNoLoginScore = totalUsers == 0 ? 100 : activeLongNoLoginCount == 0 ? 100 : (int) Math.max(0, 100 - activeLongNoLoginCount * 100 / totalUsers);
        int inactiveRecentScore = inactiveRecentLoginCount == 0 ? 100 : (int) Math.max(0, 100 - inactiveRecentLoginCount * 50 / Math.max(1, totalUsers));

        var factors = java.util.List.of(
                new SecurityScoreFactorDto("2-Step Verification", withoutTwoFactor == 0 ? "Alle actieve gebruikers hebben 2FA" : withoutTwoFactor + " van " + totalUsers + " actieve gebruikers zonder 2FA", 50, twoFaScore, 100, severity(twoFaScore)),
                new SecurityScoreFactorDto("Actieve gebruikers zonder lange inactiviteit", activeLongNoLoginCount == 0 ? "Geen actieve gebruikers met >1 jaar geen login" : activeLongNoLoginCount + " actieve gebruiker(s) met >1 jaar geen login", 25, activeNoLoginScore, 100, severity(activeNoLoginScore)),
                new SecurityScoreFactorDto("Gedeactiveerde gebruikers met recente login", inactiveRecentLoginCount == 0 ? "Geen gedeactiveerde gebruikers met recente login" : inactiveRecentLoginCount + " gedeactiveerde gebruiker(s) met recente login (mogelijk risico)", 25, inactiveRecentScore, 100, severity(inactiveRecentScore))
        );
        String status = securityScore == 100 ? "Perfect" : securityScore >= 75 ? "Goed" : securityScore > 50 ? "Matig" : "Slecht";
        return new SecurityScoreBreakdownDto(securityScore, status, factors);
    }

    private static String severity(double score) {
        if (score >= 75) return "success";
        if (score >= 50) return "warning";
        return "error";
    }

    private int calculateSecurityScore(List<User> googleUsers) {
        if (googleUsers.isEmpty()) return 100;
        long complyCount = googleUsers.stream().filter(user -> {
            boolean isActive = !Boolean.TRUE.equals(user.getSuspended());
            boolean twoFAEnabled = Boolean.TRUE.equals(user.getIsEnrolledIn2Sv());
            return GoogleServiceHelperMethods.checkUserSecurityStatus(isActive, user.getLastLoginTime(), twoFAEnabled);
        }).count();
        return (int) Math.floor((double) complyCount / googleUsers.size() * 100);
    }
}
