package com.cloudmen.cloudguard.service;

import com.cloudmen.cloudguard.dto.password.SecurityScoreFactorDto;
import com.cloudmen.cloudguard.service.cache.GoogleUsersCacheService;
import com.google.api.services.admin.directory.model.User;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.i18n.LocaleContextHolder;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static com.cloudmen.cloudguard.utility.GlobalTestHelper.*;
import static com.cloudmen.cloudguard.utility.GoogleUsersTestHelper.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
public class GoogleUsersServiceTest {

    @Mock
    private GoogleUsersCacheService usersCacheService;

    private GoogleUsersService service;

    @BeforeEach
    void setUp() {
        service = new GoogleUsersService(usersCacheService, getMessageSource());
    }

    @AfterEach
    void tearDown() {
        LocaleContextHolder.resetLocaleContext();
    }

    @Test
    void forceRefreshCache_delegatesToCacheService(){
        service.forceRefreshCache(ADMIN);
        verify(usersCacheService).forceRefreshCache(ADMIN);
    }

    @Test
    void getWorkspaceUsersPaged_emptyCache_returnsEmptyPageAndNoNextToken() {
        mockCacheEntry(usersCacheService, List.of(), Map.of(), Map.of());

        var page = service.getWorkspaceUsersPaged(ADMIN, null, 10, null);

        assertTrue(page.users().isEmpty());
        assertNull(page.nextPageToken());
    }

    @Test
    void getWorkspaceUsersPaged_queryFiltersByNameOrEmail() {
        List<User> users = List.of(
                createUser("alpha@example.com", "Alpha Team", false, true, false, daysAgo(5)),
                createUser("other@example.com", "Beta Club", false, true, false, daysAgo(5)),
                createUser("beta.user@example.com", "Charlie", false, true, false, daysAgo(5))
        );
        mockCacheEntry(usersCacheService, users, Map.of(), Map.of());

        var page = service.getWorkspaceUsersPaged(ADMIN, null, 10, "beta");

        assertEquals(2, page.users().size());
        assertTrue(page.users().stream().anyMatch(u -> u.getEmail().equals("other@example.com")));
        assertTrue(page.users().stream().anyMatch(u -> u.getEmail().equals("beta.user@example.com")));
    }

    @Test
    void getWorkspaceUsersPaged_blankQuery_returnsAllInAlphabeticalOrder() {
        List<User> users = List.of(
                createUser("z@x.com", "Zeta", false, true, false, daysAgo(5)),
                createUser("a@x.com", "Alpha", false, true, false, daysAgo(5))
        );
        mockCacheEntry(usersCacheService, users, Map.of(), Map.of());

        var page = service.getWorkspaceUsersPaged(ADMIN, "1", 10, " ");

        assertEquals(2, page.users().size());
        assertEquals("Alpha", page.users().get(0).getFullName());
        assertEquals("Zeta", page.users().get(1).getFullName());
    }

    @Test
    void getWorkspaceUsersPaged_paginationLogic() {
        List<User> users = new ArrayList<>();
        for (int i = 0; i < 5; i++) {
            users.add(createUser("u" + i + "@x.com", "A"+i, false, true, false, daysAgo(5)));
        }
        mockCacheEntry(usersCacheService, users, Map.of(), Map.of());

        var page1 = service.getWorkspaceUsersPaged(ADMIN, "1", 2, null);
        assertEquals(2, page1.users().size());
        assertEquals("A0", page1.users().get(0).getFullName());
        assertEquals("2", page1.nextPageToken());

        // Page 2
        var page2 = service.getWorkspaceUsersPaged(ADMIN, "2", 2, null);
        assertEquals(2, page2.users().size());
        assertEquals("A2", page2.users().get(0).getFullName());
        assertEquals("3", page2.nextPageToken());

        // Page 3 (Last page)
        var page3 = service.getWorkspaceUsersPaged(ADMIN, "3", 2, null);
        assertEquals(1, page3.users().size());
        assertEquals("A4", page3.users().get(0).getFullName());
        assertNull(page3.nextPageToken());
    }

    @Test
    void getWorkspaceUsersPaged_resolvesRolesCorrectly() {
        User u1 = createUser("admin@x.com", "Admin", false, true, true, daysAgo(5));
        User u2 = createUser("user@x.com", "User", false, true, false, daysAgo(5));

        Map<String, Long> roleAssignments = Map.of("admin@x.com", 100L);
        Map<Long, String> roleDict = Map.of(100L, "_SEED_ADMIN_ROLE");

        mockCacheEntry(usersCacheService, List.of(u1, u2), roleAssignments, roleDict);

        var page = service.getWorkspaceUsersPaged(ADMIN, null, 10, null);

        assertEquals("Super Admin", page.users().get(0).getRole());
        assertEquals("Regular User", page.users().get(1).getRole());
    }

    @Test
    void getUsersWithoutTwoFactor_filtersCorrectly() {
        List<User> users = List.of(
                createUser("ok@x.com", "A", false, true, false, daysAgo(5)),       // Active, 2FA
                createUser("bad@x.com", "B", false, false, false, daysAgo(5)),     // Active, NO 2FA
                createUser("susp@x.com", "C", true, false, false, daysAgo(5))      // Suspended, NO 2FA
        );
        mockCacheEntry(usersCacheService, users, Map.of(), Map.of());

        var response = service.getUsersWithoutTwoFactor(ADMIN);

        assertEquals(1, response.users().size());
        assertEquals("bad@x.com", response.users().get(0).email());
    }

    @Test
    void getUsersPageOverview_noUsers_perfectScore() {
        mockCacheEntry(usersCacheService, List.of(), Map.of(), Map.of());

        var overview = service.getUsersPageOverview(ADMIN);

        assertEquals(0, overview.totalUsers());
        assertEquals(100, overview.securityScore());
        assertEquals("perfect", overview.securityScoreBreakdown().status());
        assertFalse(overview.warnings().hasWarnings());
    }

     @Test
    void getUsersPageOverview_calculatesCountsAndScores() {
         List<User> users = List.of(
                 createUser("1@x.com", "A1", false, true, true, daysAgo(10)),
                 createUser("2@x.com", "A2", false, false, false, daysAgo(10)), // No 2FA
                 createUser("3@x.com", "A3", false, true, false, daysAgo(400)), // Long no login
                 createUser("4@x.com", "A4", true, false, false, daysAgo(2))    // Suspended recent login
         );

         mockCacheEntry(usersCacheService, users, Map.of(), Map.of());

         var overview = service.getUsersPageOverview(ADMIN);

         assertEquals(4, overview.totalUsers());
         assertEquals(1, overview.withoutTwoFactor());
         assertEquals(1, overview.adminUsers());
         assertEquals(1, overview.activeLongNoLoginCount());
         assertEquals(1, overview.inactiveRecentLoginCount());

         assertTrue(overview.warnings().hasWarnings());
         assertTrue(overview.warnings().items().get("twoFactorWarning"));
         assertTrue(overview.warnings().items().get("activeWithLongNoLogin"));
         assertTrue(overview.warnings().items().get("notActiveWithRecentLogin"));

         assertTrue(overview.securityScore() < 100);
         assertNotEquals("perfect", overview.securityScoreBreakdown().status());
     }

     @Test
    void getUsersPageOverview_withDisabledPreferences_forcesScore100AndMutedBreakdown() {
         List<User> users = List.of(
                 createUser("bad@x.com", "A", false, false, false, daysAgo(400)),
                 createUser("susp@x.com", "B", true, false, false, daysAgo(1))
         );

         mockCacheEntry(usersCacheService, users, Map.of(), Map.of());

         Set<String> disabledPrefs = Set.of("users-groups:2fa", "users-groups:activity");
        var overview = service.getUsersPageOverview(ADMIN, disabledPrefs);

        assertEquals(2, overview.totalUsers());
        assertEquals(1, overview.withoutTwoFactor());

        assertEquals(100, overview.securityScore());

        assertTrue(overview.securityScoreBreakdown().factors().stream().allMatch(SecurityScoreFactorDto::muted));

        assertNotNull(overview.warnings());
        assertFalse(overview.warnings().hasWarnings());
    }
}
