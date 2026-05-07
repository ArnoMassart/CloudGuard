package com.cloudmen.cloudguard.service.users;

import com.cloudmen.cloudguard.dto.password.SecurityScoreBreakdownDto;
import com.cloudmen.cloudguard.dto.preferences.SectionWarningsDto;
import com.cloudmen.cloudguard.dto.users.*;
import com.cloudmen.cloudguard.service.preference.SecurityPreferenceScoreSupport;
import com.cloudmen.cloudguard.service.preference.SectionWarningEvaluator;
import com.cloudmen.cloudguard.repository.UserRepository;
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
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * The main orchestration service for managing Google Workspace user data. <p>
 *
 * This service handles retrieving user lists, applying search filters and pagination, and generating security
 * overviews. It delegates heavy calculations and mapping to the {@link UsersComplianceScorer} and
 * {@link GoogleUserMapper}.
 */
@Service
public class GoogleUsersService {
    private final GoogleUsersCacheService usersCacheService;
    private final UsersComplianceScorer scorer;
    private final GoogleUserMapper mapper;
    private final UserRepository userRepository;

    public GoogleUsersService(
            GoogleUsersCacheService usersCacheService,
            UsersComplianceScorer scorer,
            GoogleUserMapper mapper,
            UserRepository userRepository) {
        this.usersCacheService = usersCacheService;
        this.scorer = scorer;
        this.mapper = mapper;
        this.userRepository = userRepository;
    }

    /**
     * Triggers a manual background refresh of the Users cache.
     *
     * @param loggedInEmail the email of the authenticated user
     */
    public void forceRefreshCache(String loggedInEmail) {
        usersCacheService.forceRefreshCache(loggedInEmail);
    }

    /**
     * Retrieves a paginated and optionally filtered list of Workspace users.
     *
     * @param loggedInEmail the email of the authenticated user
     * @param pageToken     the string representation of the requested page number
     * @param size          the maximum number of users per page
     * @param query         an optional search string (filters by name or email)
     * @return a {@link UserPageResponse} containing the requested users
     */
    public UserPageResponse getWorkspaceUsersPaged(String loggedInEmail, String pageToken, int size, String query) {
        UserCacheEntry cachedData = usersCacheService.getOrFetchData(loggedInEmail);

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

        Map<String, String> pictureFallbackByEmailLower = pictureFallbackByEmailLower(pagedGoogleUsers);

        List<UserOrgDetail> mappedUsers = pagedGoogleUsers.stream()
                .map(user -> mapper.mapToOrgDetail(
                        user,
                        cachedData.userRoleAssignments(),
                        cachedData.roleDictionary(),
                        user.getPrimaryEmail() == null
                                ? null
                                : pictureFallbackByEmailLower.get(user.getPrimaryEmail().toLowerCase(Locale.ROOT))))
                .toList();

        String nextTokenToReturn = (endIndex < totalUsers) ? String.valueOf(page + 1) : null;
        return new UserPageResponse(mappedUsers, nextTokenToReturn);
    }

    /**
     * CloudGuard DB stores the Google OAuth {@code picture} claim per user; Directory may omit {@code thumbnailPhotoUrl}.
     * One batch lookup fills gaps for the current page only.
     */
    private Map<String, String> pictureFallbackByEmailLower(List<User> googleUsers) {
        List<String> emails = googleUsers.stream()
                .map(User::getPrimaryEmail)
                .filter(Objects::nonNull)
                .distinct()
                .toList();
        if (emails.isEmpty()) {
            return Map.of();
        }
        return userRepository.findByEmailIn(emails).stream()
                .filter(u -> u.getPictureUrl() != null && !u.getPictureUrl().isBlank())
                .collect(Collectors.toMap(
                        u -> u.getEmail().toLowerCase(Locale.ROOT),
                        com.cloudmen.cloudguard.domain.model.User::getPictureUrl,
                        (a, b) -> a));
    }

    /**
     * Identifies all active users who do not have Two-Factor Authentication enabled.
     *
     * @param loggedInEmail the email of the authenticated user
     * @return a {@link UsersWithoutTwoFactorResponse} containing the non-compliant users
     */
    public UsersWithoutTwoFactorResponse getUsersWithoutTwoFactor(String loggedInEmail) {
        UserCacheEntry cachedData = usersCacheService.getOrFetchData(loggedInEmail);
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

    /**
     * Retrieves a high-level security overview of all users, evaluating factors such as 2FA enrollment and unusual
     * login activity.
     *
     * @param loggedInEmail the email of the authenticated user
     * @param disabledKeys  set of security checks to ignore based on preferences
     * @return a {@link UserOverviewResponse} with aggregated security metrics
     */
    public UserOverviewResponse getUsersPageOverview(String loggedInEmail, Set<String> disabledKeys) {
        Set<String> off = disabledKeys == null ? Set.of() : disabledKeys;
        UserCacheEntry cachedData = usersCacheService.getOrFetchData(loggedInEmail);
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

        SectionWarningsDto warnings = SectionWarningEvaluator.with(off)
                .check("twoFactorWarning", (int) withoutTwoFactor, "users-groups", "2fa")
                .check("activeWithLongNoLogin", (int) activeLongNoLoginCount, "users-groups", "activity")
                .check("notActiveWithRecentLogin", (int) inactiveRecentLoginCount, "users-groups", "activity")
                .build();

        if (totalUsers == 0) {
            return new UserOverviewResponse(
                    0, 0, 0, null, (int) activeLongNoLoginCount, (int) inactiveRecentLoginCount, null, warnings);
        }

        int securityScore = scorer.calculateSecurityScoreWithPreferenceMask(googleUsers, ignore2fa, ignoreActivity);
        SecurityScoreBreakdownDto breakdown = scorer.buildUsersBreakdown(googleUsers, (int) totalUsers, securityScore, ignore2fa, ignoreActivity);

        return new UserOverviewResponse((int) totalUsers, (int) withoutTwoFactor, (int) adminUsers, securityScore,
                (int) activeLongNoLoginCount, (int) inactiveRecentLoginCount, breakdown, warnings);
    }
}
