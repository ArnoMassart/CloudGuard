package com.cloudmen.cloudguard.integration.service;

import com.cloudmen.cloudguard.dto.oauth.OAuthCacheEntry;
import com.cloudmen.cloudguard.dto.oauth.OAuthOverviewResponse;
import com.cloudmen.cloudguard.dto.oauth.OAuthPagedResponse;
import com.cloudmen.cloudguard.dto.oauth.RawUserToken;
import com.cloudmen.cloudguard.service.AdminSecurityKeysService;
import com.cloudmen.cloudguard.service.GoogleOAuthService;
import com.cloudmen.cloudguard.service.UserService;
import com.cloudmen.cloudguard.service.cache.GoogleOAuthCacheService;
import com.cloudmen.cloudguard.utility.GoogleApiFactory;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.MessageSource;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

import java.util.List;
import java.util.Locale;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@SpringBootTest(classes = {GoogleOAuthService.class})
public class GoogleOAuthServiceIntegrationTest {

    @Autowired
    private GoogleOAuthService googleOAuthService;

    @MockitoBean
    private GoogleOAuthCacheService oAuthCacheService;

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
        googleOAuthService.forceRefreshCache(EMAIL);
        verify(oAuthCacheService).forceRefreshCache(EMAIL);
    }

    @Test
    void getOAuthPageOverview_success_calculatesTotalsAndRiskScore() {
        RawUserToken safeApp = new RawUserToken("user1@test.com", "client-1", "Safe App", List.of("userinfo.profile"), false, false);
        RawUserToken riskyApp1 = new RawUserToken("user1@test.com", "client-2", "Risky App", List.of("https://www.googleapis.com/auth/gmail.modify"), false, false);
        RawUserToken riskyApp2 = new RawUserToken("user2@test.com", "client-2", "Risky App", List.of("https://www.googleapis.com/auth/gmail.modify"), false, false);

        RawUserToken internalApp = new RawUserToken("user1@test.com", "1046230096039-0c4fqtecqs3fe7t762cn1922garmr4p5.apps.googleusercontent.com", "CloudGuard", List.of("auth/admin.directory"), false, false);

        OAuthCacheEntry cacheEntry = new OAuthCacheEntry(
                List.of(safeApp, riskyApp1, riskyApp2, internalApp),
                10,
                System.currentTimeMillis()
        );

        when(oAuthCacheService.getOrFetchOAuthData(EMAIL)).thenReturn(cacheEntry);

        OAuthOverviewResponse response = googleOAuthService.getOAuthPageOverview(EMAIL, Set.of());

        assertNotNull(response);
        assertEquals(2, response.totalThirdPartyApps());
        assertEquals(1, response.totalHighRiskApps());
        assertEquals(4, response.totalPermissionsGranted());
        assertEquals(50, response.securityScore());

        assertNotNull(response.securityScoreBreakdown());
        assertEquals("bad", response.securityScoreBreakdown().status());
    }

    @Test
    void getOAuthPageOverview_withDisabledPreferences_ignoresHighRiskPenalty() {
        RawUserToken riskyApp = new RawUserToken("user1@test.com", "client-1", "Risky App", List.of("/auth/drive"), false, false);

        OAuthCacheEntry cacheEntry = new OAuthCacheEntry(
                List.of(riskyApp),
                10,
                System.currentTimeMillis()
        );

        when(oAuthCacheService.getOrFetchOAuthData(EMAIL)).thenReturn(cacheEntry);

        Set<String> disabledKeys = Set.of("app-access:highRisk");
        OAuthOverviewResponse response = googleOAuthService.getOAuthPageOverview(EMAIL, disabledKeys);

        assertNotNull(response);
        assertEquals(1, response.totalHighRiskApps());
        assertEquals(100, response.securityScore());
        assertTrue(response.securityScoreBreakdown().factors().get(0).muted());
    }

    @Test
    void getOAuthPaged_noFilters_returnsAggregatedAndPagedResults() {
        RawUserToken app1User1 = new RawUserToken("user1@test.com", "client-1", "App Alpha", List.of("openid"), false, false);
        RawUserToken app1User2 = new RawUserToken("user2@test.com", "client-1", "App Alpha", List.of("openid"), false, false);
        RawUserToken app2 = new RawUserToken("user1@test.com", "client-2", "App Beta", List.of("/auth/drive"), false, false);

        OAuthCacheEntry cacheEntry = new OAuthCacheEntry(
                List.of(app1User1, app1User2, app2),
                10,
                System.currentTimeMillis()
        );

        when(oAuthCacheService.getOrFetchOAuthData(EMAIL)).thenReturn(cacheEntry);

        OAuthPagedResponse response = googleOAuthService.getOAuthPaged(EMAIL, null, 10, null, null);

        assertNotNull(response);
        assertEquals(2, response.allFilteredApps());
        assertEquals(1, response.allHighRiskApps());
        assertEquals(1, response.allNotHighRiskApps());
        assertEquals(2, response.apps().size());

        assertEquals("client-1", response.apps().get(0).id());
        assertEquals(2, response.apps().get(0).totalUsers());
        assertEquals(20, response.apps().get(0).exposurePercentage());
        assertFalse(response.apps().get(0).isHighRisk());

        assertEquals("client-2", response.apps().get(1).id());
        assertEquals(1, response.apps().get(1).totalUsers());
        assertTrue(response.apps().get(1).isHighRisk());
    }

    @Test
    void getOAuthPaged_withQueryFilter_returnsFilteredResults() {
        RawUserToken app1 = new RawUserToken("user1@test.com", "client-1", "App Alpha", List.of("openid"), false, false);
        RawUserToken app2 = new RawUserToken("user1@test.com", "client-2", "App Beta", List.of("auth/drive"), false, false);

        OAuthCacheEntry cacheEntry = new OAuthCacheEntry(
                List.of(app1, app2),
                10,
                System.currentTimeMillis()
        );

        when(oAuthCacheService.getOrFetchOAuthData(EMAIL)).thenReturn(cacheEntry);

        OAuthPagedResponse response = googleOAuthService.getOAuthPaged(EMAIL, "1", 10, "beta", null);

        assertNotNull(response);
        assertEquals(1, response.apps().size());
        assertEquals("client-2", response.apps().get(0).id());
    }

    @Test
    void getOAuthPaged_withRiskFilter_returnsFilteredResults() {
        RawUserToken safeApp = new RawUserToken("user1@test.com", "client-1", "App Alpha", List.of("openid"), false, false);
        RawUserToken riskyApp = new RawUserToken("user1@test.com", "client-2", "App Beta", List.of("/auth/drive"), false, false);

        OAuthCacheEntry cacheEntry = new OAuthCacheEntry(
                List.of(safeApp, riskyApp),
                10,
                System.currentTimeMillis()
        );

        when(oAuthCacheService.getOrFetchOAuthData(EMAIL)).thenReturn(cacheEntry);

        OAuthPagedResponse highRiskResponse = googleOAuthService.getOAuthPaged(EMAIL, null, 10, null, "high");
        assertEquals(1, highRiskResponse.apps().size());
        assertEquals("client-2", highRiskResponse.apps().get(0).id());

        OAuthPagedResponse lowRiskResponse = googleOAuthService.getOAuthPaged(EMAIL, null, 10, null, "not-high");
        assertEquals(1, lowRiskResponse.apps().size());
        assertEquals("client-1", lowRiskResponse.apps().get(0).id());
    }

    @Test
    void getOAuthPaged_pagingLogic_returnsCorrectPages() {
        RawUserToken app1 = new RawUserToken("u1", "c1", "A", List.of("s"), false, false);
        RawUserToken app2 = new RawUserToken("u1", "c2", "B", List.of("s"), false, false);
        RawUserToken app3 = new RawUserToken("u1", "c3", "C", List.of("s"), false, false);

        OAuthCacheEntry cacheEntry = new OAuthCacheEntry(
                List.of(app1, app2, app3),
                10,
                System.currentTimeMillis()
        );

        when(oAuthCacheService.getOrFetchOAuthData(EMAIL)).thenReturn(cacheEntry);

        OAuthPagedResponse page1 = googleOAuthService.getOAuthPaged(EMAIL, null, 2, null, null);
        assertEquals(2, page1.apps().size());
        assertEquals("2", page1.nextPageToken());

        OAuthPagedResponse page2 = googleOAuthService.getOAuthPaged(EMAIL, "2", 2, null, null);
        assertEquals(1, page2.apps().size());
        assertNull(page2.nextPageToken());
    }

    @Test
    void getOAuthPaged_emptyClientId_ignoresToken() {
        RawUserToken emptyClient = new RawUserToken("user1@test.com", "", "App Alpha", List.of("openid"), false, false);

        OAuthCacheEntry cacheEntry = new OAuthCacheEntry(
                List.of(emptyClient),
                10,
                System.currentTimeMillis()
        );

        when(oAuthCacheService.getOrFetchOAuthData(EMAIL)).thenReturn(cacheEntry);

        OAuthPagedResponse response = googleOAuthService.getOAuthPaged(EMAIL, "1", 10, null, null);

        assertEquals(0, response.apps().size());
    }
}