package com.cloudmen.cloudguard.unit.service;

import com.cloudmen.cloudguard.domain.model.Organization;
import com.cloudmen.cloudguard.domain.model.UserRole;
import com.cloudmen.cloudguard.dto.apppasswords.AppPasswordPageResponse;
import com.cloudmen.cloudguard.service.AppPasswordsService;
import com.cloudmen.cloudguard.service.OrganizationService;
import com.cloudmen.cloudguard.service.UserService;
import com.cloudmen.cloudguard.unit.helper.GlobalTestHelper;
import com.cloudmen.cloudguard.utility.GoogleApiFactory;
import com.google.api.services.admin.directory.Directory;
import com.google.api.services.admin.directory.model.Asp;
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
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class AppPasswordsServiceTest {

    @Mock
    private GoogleApiFactory apiFactory;

    @Mock
    private UserService userService;

    @Mock
    private OrganizationService organizationService;

    private ResourceBundleMessageSource messageSource;
    private AppPasswordsService service;

    @BeforeEach
    void setUp() {
        messageSource = new ResourceBundleMessageSource();
        messageSource.setBasenames("messages");
        messageSource.setDefaultEncoding(StandardCharsets.UTF_8.name());
        messageSource.setFallbackToSystemLocale(false);
        LocaleContextHolder.setLocale(Locale.ENGLISH);

        service = new AppPasswordsService(apiFactory, messageSource, userService, organizationService);

        Organization org = new Organization();
        org.setId(1L);
        org.setAdminEmail(GlobalTestHelper.ADMIN);

        com.cloudmen.cloudguard.domain.model.User adminUser = new com.cloudmen.cloudguard.domain.model.User();
        adminUser.setEmail(GlobalTestHelper.ADMIN);
        adminUser.setOrganizationId(1L);
        adminUser.setRoles(List.of(UserRole.SUPER_ADMIN));

        lenient().when(userService.findByEmailOptional(anyString()))
                .thenReturn(Optional.of(adminUser));

        lenient().when(organizationService.findById(anyLong()))
                .thenReturn(Optional.of(org));
    }

    @AfterEach
    void tearDown() {
        LocaleContextHolder.resetLocaleContext();
    }

    @Test
    void getAppPasswordsPaged_returnsAllMockUsersInOnePage() throws Exception {
        stubDirectoryWithData();

        AppPasswordPageResponse page = service.getAppPasswordsPaged(GlobalTestHelper.ADMIN, null, 100, null);

        assertEquals(5, page.users().size());
        assertNull(page.nextPageToken());
    }

    @Test
    void getAppPasswordsPaged_filtersByNameOrEmail() throws Exception {
        stubDirectoryWithData();

        AppPasswordPageResponse page = service.getAppPasswordsPaged(GlobalTestHelper.ADMIN, null, 10, "pieter");

        assertEquals(1, page.users().size());
        assertTrue(page.users().get(0).email().toLowerCase().contains("pieter"));
    }

    @Test
    void getAppPasswordsPaged_paginationAndNextToken() throws Exception {
        stubDirectoryWithData();

        AppPasswordPageResponse p1 = service.getAppPasswordsPaged(GlobalTestHelper.ADMIN, null, 2, null);
        assertEquals(2, p1.users().size());
        assertEquals("2", p1.nextPageToken());

        AppPasswordPageResponse p2 = service.getAppPasswordsPaged(GlobalTestHelper.ADMIN, "2", 2, null);
        assertEquals(2, p2.users().size());
        assertEquals("3", p2.nextPageToken());

        AppPasswordPageResponse p3 = service.getAppPasswordsPaged(GlobalTestHelper.ADMIN, "3", 2, null);
        assertEquals(1, p3.users().size());
        assertNull(p3.nextPageToken());
    }

    @Test
    void getAppPasswordsPaged_pageBeyondData_returnsEmpty() throws Exception {
        stubDirectoryWithData();

        AppPasswordPageResponse page = service.getAppPasswordsPaged(GlobalTestHelper.ADMIN, "99", 10, null);

        assertTrue(page.users().isEmpty());
        assertNull(page.nextPageToken());
    }

    @Test
    void getAppPasswordsPaged_clampsPageSizeMinimumToOne() throws Exception {
        stubDirectoryWithData();

        AppPasswordPageResponse page = service.getAppPasswordsPaged(GlobalTestHelper.ADMIN, null, 0, null);

        assertEquals(1, page.users().size());
        assertEquals("2", page.nextPageToken());
    }

    @Test
    void getAppPasswordsPaged_clampsPageSizeMaximumTo100() throws Exception {
        stubDirectoryWithData();

        AppPasswordPageResponse page = service.getAppPasswordsPaged(GlobalTestHelper.ADMIN, null, 500, null);

        assertEquals(5, page.users().size());
        assertNull(page.nextPageToken());
    }

    @Test
    void getAppPasswordsPaged_invalidPageToken_defaultsToFirstPage() throws Exception {
        stubDirectoryWithData();

        AppPasswordPageResponse page = service.getAppPasswordsPaged(GlobalTestHelper.ADMIN, "not-a-number", 2, null);

        assertEquals(2, page.users().size());
        assertEquals("2", page.nextPageToken());
    }

    @Test
    void getOverview_countsPasswordsAndUsers() throws Exception {
        stubDirectoryWithData();

        var overview = service.getOverview(GlobalTestHelper.ADMIN);

        assertTrue(overview.allowed());
        assertEquals(7, overview.totalAppPasswords());
        assertEquals(5, overview.usersWithAppPasswords());
        assertEquals(67, overview.securityScore());
        assertEquals("average", overview.securityScoreBreakdown().status());
    }

    @Test
    void getOverview_disabledAppPasswordPreference_neutralizesScore() throws Exception {
        stubDirectoryWithData();

        var overview = service.getOverview(GlobalTestHelper.ADMIN, Set.of("app-passwords:appPassword"));

        assertEquals(100, overview.securityScore());
        assertTrue(overview.securityScoreBreakdown().factors().stream()
                .allMatch(f -> f.muted()));
        assertEquals("perfect", overview.securityScoreBreakdown().status());
    }

    @Test
    void getOverview_nullDisabledKeys_treatedAsEmpty() throws Exception {
        stubDirectoryWithData();

        var overview = service.getOverview(GlobalTestHelper.ADMIN, null);

        assertEquals(67, overview.securityScore());
    }

    @Test
    void forceRefreshCache_thenPaged_usesFetchedCache() throws Exception {
        stubDirectoryWithOneUserAndEmptyAsps();

        service.forceRefreshCache(GlobalTestHelper.ADMIN);
        AppPasswordPageResponse page = service.getAppPasswordsPaged(GlobalTestHelper.ADMIN, null, 10, null);

        // Controleren dat de API daadwerkelijk is aangeroepen
        verify(apiFactory).getDirectoryService(any(Set.class), anyString());
        assertTrue(page.users().isEmpty());
    }

    @Test
    void getOverview_singleUserWithoutAppPasswords_perfectScore() throws Exception {
        stubDirectoryWithOneUserAndEmptyAsps();

        var overview = service.getOverview(GlobalTestHelper.ADMIN);

        assertEquals(0, overview.usersWithAppPasswords());
        assertEquals(0, overview.totalAppPasswords());
        assertEquals(100, overview.securityScore());
        assertEquals("perfect", overview.securityScoreBreakdown().status());
    }

    /**
     * Helper methode die 15 gebruikers met in totaal 7 app-wachtwoorden (verspreid over 5 users)
     * mockt. Dit is nodig om de berekening van score 67 te halen: 100 * (15 - 5) / 15 = 67.
     */
    private void stubDirectoryWithData() throws Exception {
        Directory directory = mock(Directory.class);
        Directory.Users usersApi = mock(Directory.Users.class);
        Directory.Users.List listCall = mock(Directory.Users.List.class);
        Directory.Asps aspsApi = mock(Directory.Asps.class);

        lenient().when(apiFactory.getDirectoryService(any(Set.class), anyString())).thenReturn(directory);
        lenient().when(directory.users()).thenReturn(usersApi);
        lenient().when(usersApi.list()).thenReturn(listCall);
        lenient().when(listCall.setCustomer(anyString())).thenReturn(listCall);
        lenient().when(listCall.setMaxResults(anyInt())).thenReturn(listCall);
        lenient().when(listCall.setFields(anyString())).thenReturn(listCall);

        List<User> mockUsers = new ArrayList<>();
        // Maak 15 gebruikers aan
        for (int i = 1; i <= 15; i++) {
            User u = new User();
            u.setId("u-" + i);
            u.setPrimaryEmail(i == 3 ? "pieter@test.com" : "user" + i + "@test.com");
            u.setName(new UserName().setFullName(i == 3 ? "Pieter Post" : "User " + i));
            u.setIsAdmin(false);
            u.setIsEnrolledIn2Sv(true);
            mockUsers.add(u);
        }

        Users listResponse = new Users();
        listResponse.setUsers(mockUsers);
        listResponse.setNextPageToken(null);
        lenient().when(listCall.execute()).thenReturn(listResponse);

        lenient().when(directory.asps()).thenReturn(aspsApi);

        // Mock ASPs calls voor elke gebruiker
        for (int i = 1; i <= 15; i++) {
            Directory.Asps.List aspsListCall = mock(Directory.Asps.List.class);
            String email = mockUsers.get(i - 1).getPrimaryEmail();
            lenient().when(aspsApi.list(email)).thenReturn(aspsListCall);

            Asps asps = new Asps();
            List<Asp> aspItems = new ArrayList<>();
            // Ken app-wachtwoorden toe aan de eerste 5 gebruikers (totaal 7 wachtwoorden)
            if (i == 1) { aspItems.add(new Asp().setCodeId(1).setName("app1")); }
            else if (i == 2) { aspItems.add(new Asp().setCodeId(2).setName("app2")); aspItems.add(new Asp().setCodeId(3).setName("app3")); }
            else if (i == 3) { aspItems.add(new Asp().setCodeId(4).setName("app4")); } // pieter
            else if (i == 4) { aspItems.add(new Asp().setCodeId(5).setName("app5")); }
            else if (i == 5) { aspItems.add(new Asp().setCodeId(6).setName("app6")); aspItems.add(new Asp().setCodeId(7).setName("app7")); }

            asps.setItems(aspItems.isEmpty() ? null : aspItems);
            lenient().when(aspsListCall.execute()).thenReturn(asps);
        }
    }

    private void stubDirectoryWithOneUserAndEmptyAsps() throws Exception {
        Directory directory = mock(Directory.class);
        Directory.Users usersApi = mock(Directory.Users.class);
        Directory.Users.List listCall = mock(Directory.Users.List.class);
        Directory.Asps aspsApi = mock(Directory.Asps.class);
        Directory.Asps.List aspsListCall = mock(Directory.Asps.List.class);

        lenient().when(apiFactory.getDirectoryService(any(Set.class), anyString())).thenReturn(directory);
        lenient().when(directory.users()).thenReturn(usersApi);
        lenient().when(usersApi.list()).thenReturn(listCall);
        lenient().when(listCall.setCustomer(anyString())).thenReturn(listCall);
        lenient().when(listCall.setMaxResults(anyInt())).thenReturn(listCall);
        lenient().when(listCall.setFields(anyString())).thenReturn(listCall);

        User googleUser = new User();
        googleUser.setId("u-1");
        googleUser.setPrimaryEmail("user@bedrijf.nl");
        googleUser.setName(new UserName().setFullName("Test User"));
        googleUser.setIsAdmin(false);
        googleUser.setIsEnrolledIn2Sv(true);

        Users listResponse = new Users();
        listResponse.setUsers(List.of(googleUser));
        listResponse.setNextPageToken(null);
        lenient().when(listCall.execute()).thenReturn(listResponse);

        lenient().when(directory.asps()).thenReturn(aspsApi);
        lenient().when(aspsApi.list(anyString())).thenReturn(aspsListCall);

        Asps emptyAsps = new Asps();
        emptyAsps.setItems(List.of());
        lenient().when(aspsListCall.execute()).thenReturn(emptyAsps);
    }
}