package com.cloudmen.cloudguard.unit.service;

import com.cloudmen.cloudguard.dto.apppasswords.AppPasswordPageResponse;
import com.cloudmen.cloudguard.service.AppPasswordsService;
import com.cloudmen.cloudguard.unit.helper.GlobalTestHelper;
import com.cloudmen.cloudguard.utility.GoogleApiFactory;
import com.google.api.services.admin.directory.Directory;
import com.google.api.services.admin.directory.model.Asps;
import com.google.api.services.admin.directory.model.User;
import com.google.api.services.admin.directory.model.UserName;
import com.google.api.services.admin.directory.model.Users;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.support.ResourceBundleMessageSource;
import org.springframework.context.i18n.LocaleContextHolder;

import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Locale;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AppPasswordsServiceTest {

    @Mock
    GoogleApiFactory apiFactory;

    private ResourceBundleMessageSource messageSource;
    private AppPasswordsService service;

    @BeforeEach
    void setUp() {
        messageSource = new ResourceBundleMessageSource();
        messageSource.setBasenames("messages");
        messageSource.setDefaultEncoding(StandardCharsets.UTF_8.name());
        messageSource.setFallbackToSystemLocale(false);
        LocaleContextHolder.setLocale(Locale.ENGLISH);
        service = new AppPasswordsService(apiFactory, messageSource);
    }

    @AfterEach
    void tearDown() {
        LocaleContextHolder.resetLocaleContext();
    }

    @Test
    void getAppPasswordsPaged_testMode_returnsAllMockUsersInOnePage() {
        AppPasswordPageResponse page = service.getAppPasswordsPaged(GlobalTestHelper.ADMIN, null, 100, null, true);

        assertEquals(5, page.users().size());
        assertNull(page.nextPageToken());
    }

    @Test
    void getAppPasswordsPaged_testMode_filtersByNameOrEmail() {
        AppPasswordPageResponse page = service.getAppPasswordsPaged(GlobalTestHelper.ADMIN, null, 10, "pieter", true);

        assertEquals(1, page.users().size());
        assertTrue(page.users().get(0).email().toLowerCase().contains("pieter"));
    }

    @Test
    void getAppPasswordsPaged_testMode_paginationAndNextToken() {
        AppPasswordPageResponse p1 = service.getAppPasswordsPaged(GlobalTestHelper.ADMIN, null, 2, null, true);
        assertEquals(2, p1.users().size());
        assertEquals("2", p1.nextPageToken());

        AppPasswordPageResponse p2 = service.getAppPasswordsPaged(GlobalTestHelper.ADMIN, "2", 2, null, true);
        assertEquals(2, p2.users().size());
        assertEquals("3", p2.nextPageToken());

        AppPasswordPageResponse p3 = service.getAppPasswordsPaged(GlobalTestHelper.ADMIN, "3", 2, null, true);
        assertEquals(1, p3.users().size());
        assertNull(p3.nextPageToken());
    }

    @Test
    void getAppPasswordsPaged_testMode_pageBeyondData_returnsEmpty() {
        AppPasswordPageResponse page = service.getAppPasswordsPaged(GlobalTestHelper.ADMIN, "99", 10, null, true);

        assertTrue(page.users().isEmpty());
        assertNull(page.nextPageToken());
    }

    @Test
    void getAppPasswordsPaged_testMode_clampsPageSizeMinimumToOne() {
        AppPasswordPageResponse page = service.getAppPasswordsPaged(GlobalTestHelper.ADMIN, null, 0, null, true);

        assertEquals(1, page.users().size());
        assertEquals("2", page.nextPageToken());
    }

    @Test
    void getAppPasswordsPaged_testMode_clampsPageSizeMaximumTo100() {
        AppPasswordPageResponse page = service.getAppPasswordsPaged(GlobalTestHelper.ADMIN, null, 500, null, true);

        assertEquals(5, page.users().size());
        assertNull(page.nextPageToken());
    }

    @Test
    void getAppPasswordsPaged_invalidPageToken_defaultsToFirstPage() {
        AppPasswordPageResponse page = service.getAppPasswordsPaged(GlobalTestHelper.ADMIN, "not-a-number", 2, null, true);

        assertEquals(2, page.users().size());
        assertEquals("2", page.nextPageToken());
    }

    @Test
    void getOverview_testMode_countsPasswordsAndUsers() {
        var overview = service.getOverview(GlobalTestHelper.ADMIN, true);

        assertTrue(overview.allowed());
        assertEquals(7, overview.totalAppPasswords());
        assertEquals(5, overview.usersWithAppPasswords());
        assertEquals(67, overview.securityScore());
        assertEquals("average", overview.securityScoreBreakdown().status());
    }

    @Test
    void getOverview_testMode_disabledAppPasswordPreference_neutralizesScore() {
        var overview = service.getOverview(GlobalTestHelper.ADMIN, true, Set.of("app-passwords:appPassword"));

        assertEquals(100, overview.securityScore());
        assertTrue(overview.securityScoreBreakdown().factors().stream()
                .allMatch(f -> f.muted()));
        assertEquals("perfect", overview.securityScoreBreakdown().status());
    }

    @Test
    void getOverview_testMode_nullDisabledKeys_treatedAsEmpty() {
        var overview = service.getOverview(GlobalTestHelper.ADMIN, true, null);

        assertEquals(67, overview.securityScore());
    }

    @Test
    void forceRefreshCache_thenPaged_liveMode_usesFetchedCache() throws Exception {
        stubDirectoryWithOneUserAndEmptyAsps();

        service.forceRefreshCache(GlobalTestHelper.ADMIN);
        AppPasswordPageResponse page = service.getAppPasswordsPaged(GlobalTestHelper.ADMIN, null, 10, null, false);

        verify(apiFactory).getDirectoryService(any(Set.class), eq(GlobalTestHelper.ADMIN));
        assertTrue(page.users().isEmpty());
    }

    @Test
    void getOverview_liveMode_singleUserWithoutAppPasswords_perfectScore() throws Exception {
        stubDirectoryWithOneUserAndEmptyAsps();

        var overview = service.getOverview(GlobalTestHelper.ADMIN, false);

        assertEquals(0, overview.usersWithAppPasswords());
        assertEquals(0, overview.totalAppPasswords());
        assertEquals(100, overview.securityScore());
        assertEquals("perfect", overview.securityScoreBreakdown().status());
    }

    private void stubDirectoryWithOneUserAndEmptyAsps() throws Exception {
        Directory directory = mock(Directory.class);
        Directory.Users usersApi = mock(Directory.Users.class);
        Directory.Users.List listCall = mock(Directory.Users.List.class);
        Directory.Asps aspsApi = mock(Directory.Asps.class);
        Directory.Asps.List aspsListCall = mock(Directory.Asps.List.class);

        when(apiFactory.getDirectoryService(any(Set.class), eq(GlobalTestHelper.ADMIN))).thenReturn(directory);
        when(directory.users()).thenReturn(usersApi);
        when(usersApi.list()).thenReturn(listCall);
        when(listCall.setCustomer(anyString())).thenReturn(listCall);
        when(listCall.setMaxResults(anyInt())).thenReturn(listCall);
        when(listCall.setFields(anyString())).thenReturn(listCall);

        User user = new User();
        user.setId("u-1");
        user.setPrimaryEmail("user@bedrijf.nl");
        user.setName(new UserName().setFullName("Test User"));
        user.setIsAdmin(false);
        user.setIsEnrolledIn2Sv(true);

        Users listResponse = new Users();
        listResponse.setUsers(List.of(user));
        listResponse.setNextPageToken(null);
        when(listCall.execute()).thenReturn(listResponse);

        when(directory.asps()).thenReturn(aspsApi);
        when(aspsApi.list(eq("user@bedrijf.nl"))).thenReturn(aspsListCall);
        Asps emptyAsps = new Asps();
        emptyAsps.setItems(List.of());
        when(aspsListCall.execute()).thenReturn(emptyAsps);
    }
}
