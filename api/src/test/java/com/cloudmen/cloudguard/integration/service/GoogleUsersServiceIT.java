package com.cloudmen.cloudguard.integration.service;

import com.cloudmen.cloudguard.dto.users.UserCacheEntry;
import com.cloudmen.cloudguard.dto.users.UserOverviewResponse;
import com.cloudmen.cloudguard.dto.users.UserPageResponse;
import com.cloudmen.cloudguard.dto.users.UsersWithoutTwoFactorResponse;
import com.cloudmen.cloudguard.service.AdminSecurityKeysService;
import com.cloudmen.cloudguard.service.GoogleUsersService;
import com.cloudmen.cloudguard.service.cache.GoogleUsersCacheService;
import com.cloudmen.cloudguard.utility.GoogleApiFactory;
import com.google.api.client.util.DateTime;
import com.google.api.services.admin.directory.model.User;
import com.google.api.services.admin.directory.model.UserName;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.MessageSource;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.*;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@SpringBootTest(classes = {GoogleUsersService.class})
public class GoogleUsersServiceIT {

    @Autowired
    private GoogleUsersService googleUsersService;

    @MockitoBean
    private GoogleUsersCacheService usersCacheService;

    @MockitoBean(name = "messageSource")
    private MessageSource messageSource;

    @MockitoBean
    private GoogleApiFactory googleApiFactory;

    @MockitoBean
    private AdminSecurityKeysService adminSecurityKeysService;

    private static final String EMAIL = "admin@cloudmen.com";

    @BeforeEach
    void setUp() {
        when(messageSource.getMessage(anyString(), any(), any(Locale.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));
    }

    @Test
    void forceRefreshCache_callsCacheService() {
        googleUsersService.forceRefreshCache(EMAIL);
        verify(usersCacheService).forceRefreshCache(EMAIL);
    }

    @Test
    void getWorkspaceUsersPaged_noFilters_returnsPagedResults() {
        User u1 = createUser("1", "alpha@test.com", "Alpha User", false, true, true, 0);
        User u2 = createUser("2", "beta@test.com", "Beta User", false, false, false, 0);
        User u3 = createUser("3", "gamma@test.com", "Gamma User", true, false, false, 0);

        UserCacheEntry cacheEntry = new UserCacheEntry(
                List.of(u3, u1, u2),
                Map.of(101L, "Super Admin"),
                Map.of("1", 101L),
                System.currentTimeMillis()
        );
        when(usersCacheService.getOrFetchUsersData(EMAIL)).thenReturn(cacheEntry);

        UserPageResponse page1 = googleUsersService.getWorkspaceUsersPaged(EMAIL, null, 2, null);

        assertNotNull(page1);
        assertEquals(2, page1.users().size());
        assertEquals("Alpha User", page1.users().get(0).getFullName());
        assertEquals("Super Admin", page1.users().get(0).getRole());
        assertEquals("Beta User", page1.users().get(1).getFullName());
        assertEquals("2", page1.nextPageToken());

        UserPageResponse page2 = googleUsersService.getWorkspaceUsersPaged(EMAIL, "2", 2, null);

        assertNotNull(page2);
        assertEquals(1, page2.users().size());
        assertEquals("Gamma User", page2.users().get(0).getFullName());
        assertNull(page2.nextPageToken());
    }

    @Test
    void getWorkspaceUsersPaged_withQueryFilter_returnsFilteredResults() {
        User u1 = createUser("1", "alpha@test.com", "Alpha User", false, true, false, 0);
        User u2 = createUser("2", "beta@test.com", "Beta User", false, false, false, 0);

        UserCacheEntry cacheEntry = new UserCacheEntry(List.of(u1, u2), Map.of(), Map.of(), System.currentTimeMillis());
        when(usersCacheService.getOrFetchUsersData(EMAIL)).thenReturn(cacheEntry);

        UserPageResponse response = googleUsersService.getWorkspaceUsersPaged(EMAIL, "1", 10, "alpha");

        assertNotNull(response);
        assertEquals(1, response.users().size());
        assertEquals("Alpha User", response.users().get(0).getFullName());
    }

    @Test
    void getUsersWithoutTwoFactor_returnsOnlyActiveUsersWithout2FA() {
        User safeUser = createUser("1", "safe@test.com", "Safe User", false, true, false, 0);
        User no2faUser = createUser("2", "no2fa@test.com", "No 2FA", false, false, false, 0);
        User suspendedUser = createUser("3", "susp@test.com", "Suspended", true, false, false, 0);

        UserCacheEntry cacheEntry = new UserCacheEntry(List.of(safeUser, no2faUser, suspendedUser), Map.of(), Map.of(), System.currentTimeMillis());
        when(usersCacheService.getOrFetchUsersData(EMAIL)).thenReturn(cacheEntry);

        UsersWithoutTwoFactorResponse response = googleUsersService.getUsersWithoutTwoFactor(EMAIL);

        assertNotNull(response);
        assertEquals(1, response.users().size());
        assertEquals("no2fa@test.com", response.users().get(0).email());
    }

    @Test
    void getUsersPageOverview_success_calculatesMetricsAndScores() {
        User perfect = createUser("1", "perfect@test.com", "Perfect", false, true, false, 2);
        User no2fa = createUser("2", "no2fa@test.com", "No 2FA", false, false, false, 2);
        User longNoLogin = createUser("3", "old@test.com", "Old", false, true, false, 400);
        User suspendedRecent = createUser("4", "susp@test.com", "Suspended", true, false, false, 2);

        UserCacheEntry cacheEntry = new UserCacheEntry(
                List.of(perfect, no2fa, longNoLogin, suspendedRecent), Map.of(), Map.of(), System.currentTimeMillis()
        );
        when(usersCacheService.getOrFetchUsersData(EMAIL)).thenReturn(cacheEntry);

        UserOverviewResponse response = googleUsersService.getUsersPageOverview(EMAIL);

        assertNotNull(response);
        assertEquals(4, response.totalUsers());
        assertEquals(1, response.withoutTwoFactor());
        assertEquals(1, response.activeLongNoLoginCount());
        assertEquals(1, response.inactiveRecentLoginCount());

        assertTrue(response.securityScore() < 100);

        assertNotNull(response.securityScoreBreakdown());
        assertEquals(3, response.securityScoreBreakdown().factors().size());

        assertNotNull(response.warnings());
        assertTrue(response.warnings().hasWarnings());
    }

    @Test
    void getUsersPageOverview_withDisabledPreferences_adjustsScoreAndMutesFactors() {
        User no2fa = createUser("2", "no2fa@test.com", "No 2FA", false, false, false, 2);
        User longNoLogin = createUser("3", "old@test.com", "Old", false, true, false, 400);

        UserCacheEntry cacheEntry = new UserCacheEntry(List.of(no2fa, longNoLogin), Map.of(), Map.of(), System.currentTimeMillis());
        when(usersCacheService.getOrFetchUsersData(EMAIL)).thenReturn(cacheEntry);

        Set<String> disabledKeys = Set.of("users-groups:2fa", "users-groups:activity");

        UserOverviewResponse response = googleUsersService.getUsersPageOverview(EMAIL, disabledKeys);

        assertNotNull(response);
        assertEquals(100, response.securityScore());

        assertTrue(response.securityScoreBreakdown().factors().get(0).muted());
        assertTrue(response.securityScoreBreakdown().factors().get(1).muted());
        assertTrue(response.securityScoreBreakdown().factors().get(2).muted());

        assertFalse(response.warnings().hasWarnings());
    }

    @Test
    void getUsersPageOverview_emptyUsers_returnsPerfectScore() {
        UserCacheEntry cacheEntry = new UserCacheEntry(Collections.emptyList(), Map.of(), Map.of(), System.currentTimeMillis());
        when(usersCacheService.getOrFetchUsersData(EMAIL)).thenReturn(cacheEntry);

        UserOverviewResponse response = googleUsersService.getUsersPageOverview(EMAIL, Set.of());

        assertNotNull(response);
        assertEquals(0, response.totalUsers());
        assertEquals(100, response.securityScore());
        assertEquals("perfect", response.securityScoreBreakdown().status());
    }

    private User createUser(String id, String email, String fullName, boolean isSuspended, boolean is2fa, boolean isAdmin, int loginDaysAgo) {
        User user = new User();
        user.setId(id);
        user.setPrimaryEmail(email);

        UserName name = new UserName();
        name.setFullName(fullName);
        user.setName(name);

        user.setSuspended(isSuspended);
        user.setIsEnrolledIn2Sv(is2fa);
        user.setIsAdmin(isAdmin);

        long loginMillis = Instant.now().minus(loginDaysAgo, ChronoUnit.DAYS).toEpochMilli();
        user.setLastLoginTime(new DateTime(loginMillis));

        return user;
    }
}
