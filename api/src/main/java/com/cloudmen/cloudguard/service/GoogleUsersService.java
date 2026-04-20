package com.cloudmen.cloudguard.service;

import com.cloudmen.cloudguard.dto.password.SecurityScoreBreakdownDto;
import com.cloudmen.cloudguard.dto.password.SecurityScoreFactorDto;
import com.cloudmen.cloudguard.dto.preferences.SectionWarningsDto;
import com.cloudmen.cloudguard.dto.users.*;
import com.cloudmen.cloudguard.service.preference.SecurityPreferenceScoreSupport;
import com.cloudmen.cloudguard.service.preference.SectionWarningEvaluator;
import com.cloudmen.cloudguard.service.cache.GoogleUsersCacheService;
import com.cloudmen.cloudguard.utility.DateTimeConverter;
import com.cloudmen.cloudguard.utility.GoogleServiceHelperMethods;
import com.google.api.client.util.DateTime;
import com.google.api.services.admin.directory.model.User;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.MessageSource;
import org.springframework.context.i18n.LocaleContextHolder;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Set;
import java.util.Locale;

import static com.cloudmen.cloudguard.utility.GoogleServiceHelperMethods.securityScoreFactorForDetail;
import static com.cloudmen.cloudguard.utility.GoogleServiceHelperMethods.severity;

@Service
public class GoogleUsersService {
    private final GoogleUsersCacheService usersCacheService;
    private final MessageSource messageSource;

    public GoogleUsersService(GoogleUsersCacheService usersCacheService, @Qualifier("messageSource") MessageSource messageSource) {
        this.usersCacheService = usersCacheService;
        this.messageSource = messageSource;
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
            var security = GoogleServiceHelperMethods.evaluateUserSecurity(isActive, user.getLastLoginTime(), twoFAEnabled);

            return new UserOrgDetail(
                    user.getName().getFullName(),
                    user.getPrimaryEmail(),
                    GoogleServiceHelperMethods.translateRoleName(roleName),
                    isActive,
                    user.getLastLoginTime() != null ? DateTimeConverter.convertToTimeAgo(user.getLastLoginTime()) : "Nooit",
                    twoFAEnabled,
                    security.conform(),
                    security.violationCodes()
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
        return getUsersPageOverview(loggedInEmail, Set.of());
    }

    public UserOverviewResponse getUsersPageOverview(String loggedInEmail, Set<String> disabledKeys) {
        Set<String> off = disabledKeys == null ? Set.of() : disabledKeys;
        UserCacheEntry cachedData = usersCacheService.getOrFetchUsersData(loggedInEmail);
        List<User> googleUsers = cachedData.allUsers();
        LocalDate now = LocalDate.now();

        long totalUsers = googleUsers.size();
        long withoutTwoFactor = googleUsers.stream()
                .filter(user -> !Boolean.TRUE.equals(user.getSuspended()) && !Boolean.TRUE.equals(user.getIsEnrolledIn2Sv()))
                .count();
        long adminUsers = googleUsers.stream().filter(User::getIsAdmin).count();

        long activeLongNoLoginCount = googleUsers.stream().filter(user -> {
            if (user.getLastLoginTime() == null) return false;
            LocalDate loginDate = DateTimeConverter.convertGoogleDateTimeToLocalDate(user.getLastLoginTime());
            return !Boolean.TRUE.equals(user.getSuspended()) && ChronoUnit.DAYS.between(loginDate, now) >= 90;
        }).count();

        long inactiveRecentLoginCount = googleUsers.stream().filter(user -> {
            if (user.getLastLoginTime() == null) return false;
            LocalDate loginDate = DateTimeConverter.convertGoogleDateTimeToLocalDate(user.getLastLoginTime());
            return Boolean.TRUE.equals(user.getSuspended()) && ChronoUnit.DAYS.between(loginDate, now) <= 7;
        }).count();

        boolean ignore2fa = SecurityPreferenceScoreSupport.preferenceDisabled(off, "users-groups", "2fa");
        boolean ignoreActivity = SecurityPreferenceScoreSupport.preferenceDisabled(off, "users-groups", "activity");

        int securityScore = calculateSecurityScoreWithPreferenceMask(googleUsers, ignore2fa, ignoreActivity);
        SecurityScoreBreakdownDto breakdown = buildUsersBreakdown(googleUsers, (int) totalUsers, securityScore, ignore2fa, ignoreActivity);

        SectionWarningsDto warnings = SectionWarningEvaluator.with(off)
                .check("twoFactorWarning", (int) withoutTwoFactor, "users-groups", "2fa")
                .check("activeWithLongNoLogin", (int) activeLongNoLoginCount, "users-groups", "activity")
                .check("notActiveWithRecentLogin", (int) inactiveRecentLoginCount, "users-groups", "activity")
                .build();

        return new UserOverviewResponse((int) totalUsers, (int) withoutTwoFactor, (int) adminUsers, securityScore,
                (int) activeLongNoLoginCount, (int) inactiveRecentLoginCount, breakdown, warnings);
    }

    /**
     * Each non-compliant user is attributed to exactly one failure reason (in order of checks).
     */
    private SecurityScoreBreakdownDto buildUsersBreakdown(List<User> googleUsers, int totalUsers, int securityScore,
                                                         boolean ignore2fa, boolean ignoreActivity) {
        LocalDate now = LocalDate.now();
        int no2FACount = 0;
        int longNoLoginCount = 0;
        int inactiveRecentCount = 0;

        for (User user : googleUsers) {
            boolean isActive = !Boolean.TRUE.equals(user.getSuspended());
            boolean twoFAEnabled = Boolean.TRUE.equals(user.getIsEnrolledIn2Sv());
            DateTime lastLogin = user.getLastLoginTime();

            if (isActive && !twoFAEnabled) {
                no2FACount++;
                continue;
            }
            if (isActive) {
                if (lastLogin == null) {
                    longNoLoginCount++;
                } else {
                    LocalDate loginDate = DateTimeConverter.convertGoogleDateTimeToLocalDate(lastLogin);
                    if (ChronoUnit.YEARS.between(loginDate, now) >= 1) {
                        longNoLoginCount++;
                    }
                }
                continue;
            }
            if (lastLogin != null) {
                LocalDate loginDate = DateTimeConverter.convertGoogleDateTimeToLocalDate(lastLogin);
                if (ChronoUnit.DAYS.between(loginDate, now) <= 7) {
                    inactiveRecentCount++;
                }
            }
        }

        int score1 = totalUsers == 0 ? 100 : (int) Math.round(100.0 * (totalUsers - no2FACount) / totalUsers);
        int score2 = totalUsers == 0 ? 100 : (int) Math.round(100.0 * (totalUsers - longNoLoginCount) / totalUsers);
        int score3 = totalUsers == 0 ? 100 : (int) Math.round(100.0 * (totalUsers - inactiveRecentCount) / totalUsers);

        if (ignore2fa) {
            score1 = 100;
        }
        if (ignoreActivity) {
            score2 = 100;
            score3 = 100;
        }

        Locale locale = LocaleContextHolder.getLocale();

        boolean show2faFactor = totalUsers > 0 || ignore2fa;
        boolean showActivityFactors = totalUsers > 0 || ignoreActivity;

        var factors = java.util.List.of(
                securityScoreFactorForDetail(
                        show2faFactor,
                        messageSource.getMessage("users.overview.2step-title", null, locale),
                        no2FACount == 0 ? messageSource.getMessage("users.overview.2step.compliant", null, locale) : messageSource.getMessage("users.overview.2step.non_compliant", new Object[]{no2FACount}, locale),
                        score1,
                        100,
                        severity(score1),
                        ignore2fa),
                securityScoreFactorForDetail(
                        showActivityFactors,
                        messageSource.getMessage("users.overview.activeLongNoLogin-title", null, locale),
                        longNoLoginCount == 0 ? messageSource.getMessage("users.overview.no_login.compliant", null, locale) : messageSource.getMessage("users.overview.no_login.non_compliant", new Object[]{longNoLoginCount}, locale),
                        score2,
                        100,
                        severity(score2),
                        ignoreActivity),
                securityScoreFactorForDetail(
                        showActivityFactors,
                        messageSource.getMessage("users.overview.deactivatedRecentLogin-title", null, locale),
                        inactiveRecentCount == 0 ? messageSource.getMessage("users.overview.recent_login.compliant", null, locale) : messageSource.getMessage("users.overview.recent_login.non_compliant", new Object[]{inactiveRecentCount}, locale),
                        score3,
                        100,
                        severity(score3),
                        ignoreActivity)
        );
        String status = securityScore == 100 ? "perfect" : securityScore >= 75 ? "good" : securityScore > 50 ? "average" : "bad";
        return new SecurityScoreBreakdownDto(securityScore, status, factors);
    }

    private int calculateSecurityScoreWithPreferenceMask(List<User> googleUsers, boolean ignore2fa, boolean ignoreActivity) {
        if (googleUsers.isEmpty()) {
            return 100;
        }
        int parts = 0;
        long sum = 0;
        for (User user : googleUsers) {
            boolean isActive = !Boolean.TRUE.equals(user.getSuspended());
            boolean twoFAEnabled = Boolean.TRUE.equals(user.getIsEnrolledIn2Sv());

            if (!ignore2fa) {
                parts++;
                sum += (!isActive || twoFAEnabled) ? 1 : 0;
            }
            if (!ignoreActivity) {
                parts++;
                sum += activityMeasuresComply(isActive, user.getLastLoginTime()) ? 1 : 0;
            }
        }
        if (parts == 0) {
            return 100;
        }
        return (int) Math.floor((double) sum / parts * 100);
    }

    private static boolean activityMeasuresComply(boolean isActive, DateTime lastLogin) {
        LocalDate now = LocalDate.now();
        if (isActive) {
            if (lastLogin == null) {
                return false;
            }
            LocalDate loginDate = DateTimeConverter.convertGoogleDateTimeToLocalDate(lastLogin);
            return ChronoUnit.YEARS.between(loginDate, now) < 1;
        }
        if (lastLogin == null) {
            return true;
        }
        LocalDate loginDate = DateTimeConverter.convertGoogleDateTimeToLocalDate(lastLogin);
        return ChronoUnit.DAYS.between(loginDate, now) > 7;
    }
}
