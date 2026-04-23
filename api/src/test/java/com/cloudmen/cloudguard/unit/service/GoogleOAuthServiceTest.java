package com.cloudmen.cloudguard.unit.service;

import com.cloudmen.cloudguard.service.GoogleOAuthService;
import com.cloudmen.cloudguard.service.cache.GoogleOAuthCacheService;
import com.cloudmen.cloudguard.dto.password.SecurityScoreFactorDto;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.i18n.LocaleContextHolder;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import static com.cloudmen.cloudguard.unit.helper.GlobalTestHelper.*;
import static com.cloudmen.cloudguard.unit.helper.GoogleOAuthTestHelper.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
public class GoogleOAuthServiceTest {
    @Mock
    private GoogleOAuthCacheService oAuthCacheService;

    private GoogleOAuthService service;

    @BeforeEach
    void setUp() {
        service = new GoogleOAuthService(oAuthCacheService, getMessageSource());
    }

    @AfterEach
    void tearDown() {
        LocaleContextHolder.resetLocaleContext();
    }

    @Test
    void forceRefreshCache_delegatesToCacheService() {
        service.forceRefreshCache(ADMIN);
        verify(oAuthCacheService).forceRefreshCache(ADMIN);
    }

    @Test
    void getOAuthPaged_emptyCache_returnsEmptyPageAndNoNextToken() {
        mockCacheEntry(oAuthCacheService, List.of(), 10);

        var page = service.getOAuthPaged(ADMIN, null, 10, null, null);

        assertTrue(page.apps().isEmpty());
        assertNull(page.nextPageToken());
    }

    @Test
    void getOAuthPaged_aggregatesTokensByClientId_andCalculatesExposure() {
        var token1 = createToken("client1", "Test App", "user1@x.com", List.of("openid"), false, false);
        var token2 = createToken("client1", "Test App", "user2@x.com", List.of("userinfo.email"), false, false);
        // Een andere app
        var token3 = createToken("client2", "Other App", "user1@x.com", List.of("openid"), false, false);

        mockCacheEntry(oAuthCacheService, List.of(token1, token2, token3), 4);

        var page = service.getOAuthPaged(ADMIN, null, 10, null, null);

        assertEquals(2, page.apps().size());

        System.out.println("DE APPS IN DE LIJST ZIJN: " + page.apps());

        var testApp = page.apps().stream().filter(a -> a.id().equals("client1")).findFirst().get();
        assertEquals(2, testApp.totalUsers());
        assertEquals(50, testApp.exposurePercentage());
        assertEquals(2, testApp.scopeCount());
    }

    @Test
    void getOAuthPaged_filtersByQueryName() {
        var token1 = createToken("c1", "Zoom", "u@x.com", List.of(), false, false);
        var token2 = createToken("c2", "Slack", "u@x.com", List.of(), false, false);

        mockCacheEntry(oAuthCacheService, List.of(token1, token2), 10);

        var page = service.getOAuthPaged(ADMIN, null, 10, "sla", null);

        assertEquals(1, page.apps().size());
        assertEquals("Slack", page.apps().get(0).name());
    }

    @Test
    void getOAuthPaged_filtersByRisk() {
        var tokenHigh = createToken("c1", "High Risk App", "u@x.com", List.of("/auth/admin.directory"), false, false);
        var tokenLow = createToken("c2", "Low Risk App", "u@x.com", List.of("openid"), false, false);

        mockCacheEntry(oAuthCacheService, List.of(tokenHigh, tokenLow), 10);

        // Filter op 'high'
        var pageHigh = service.getOAuthPaged(ADMIN, null, 10, null, "high");
        assertEquals(1, pageHigh.apps().size());
        assertEquals("High Risk App", pageHigh.apps().get(0).name());
        assertTrue(pageHigh.apps().get(0).isHighRisk());

        // Filter op 'not-high'
        var pageLow = service.getOAuthPaged(ADMIN, null, 10, null, "not-high");
        assertEquals(1, pageLow.apps().size());
        assertEquals("Low Risk App", pageLow.apps().get(0).name());
        assertFalse(pageLow.apps().get(0).isHighRisk());
    }

    @Test
    void getOAuthPaged_paginationLogic() {
        List<com.cloudmen.cloudguard.dto.oauth.RawUserToken> tokens = new ArrayList<>();
        // Create 5 distinct apps (different clientIds)
        for (int i = 0; i < 5; i++) {
            tokens.add(createToken("c" + i, "App " + i, "u@x.com", List.of(), false, false));
        }

        mockCacheEntry(oAuthCacheService, tokens, 10);

        // Page 1
        var page1 = service.getOAuthPaged(ADMIN, "1", 2, null, null);
        assertEquals(2, page1.apps().size());
        assertEquals("2", page1.nextPageToken());

        // Page 3 (Last page)
        var page3 = service.getOAuthPaged(ADMIN, "3", 2, null, null);
        assertEquals(1, page3.apps().size());
        assertNull(page3.nextPageToken());
    }

    @Test
    void getOAuthPageOverview_noApps_perfectScore() {
        mockCacheEntry(oAuthCacheService, List.of(), 10);

        var overview = service.getOAuthPageOverview(ADMIN, Set.of());

        assertEquals(0, overview.totalThirdPartyApps());
        assertEquals(100, overview.securityScore());
        assertEquals("perfect", overview.securityScoreBreakdown().status());
    }

    @Test
    void getOAuthPageOverview_calculatesCountsAndScores() {
        var internalToken = createToken(INTERNAL_CLIENT_ID, "CloudGuard", "u@x.com", List.of("/auth/admin.directory"), false, false);
        var highRiskToken = createToken("c2", "Bad App", "u@x.com", List.of("/auth/gmail"), false, false);
        var lowRiskToken = createToken("c3", "Good App", "u@x.com", List.of("openid"), false, false);

        mockCacheEntry(oAuthCacheService, List.of(internalToken, highRiskToken, lowRiskToken), 10);

        var overview = service.getOAuthPageOverview(ADMIN, Set.of());

        assertEquals(2, overview.totalThirdPartyApps());
        assertEquals(3, overview.totalPermissionsGranted());
        assertEquals(2, overview.totalHighRiskApps());
        assertEquals(0, overview.securityScore());
        assertNotEquals("perfect", overview.securityScoreBreakdown().status());
    }

    @Test
    void getOAuthPageOverview_withDisabledPreferences_forcesScore100AndMutedBreakdown() {
        var highRiskToken = createToken("c1", "Very Bad App", "u@x.com", List.of("/auth/admin.directory", "/auth/drive"), false, false);

        mockCacheEntry(oAuthCacheService, List.of(highRiskToken), 10);

        var overview = service.getOAuthPageOverview(ADMIN, Set.of("app-access:highRisk"));

        assertEquals(1, overview.totalThirdPartyApps());
        assertEquals(1, overview.totalHighRiskApps());
        assertEquals(100, overview.securityScore());

        assertTrue(overview.securityScoreBreakdown().factors().stream().anyMatch(SecurityScoreFactorDto::muted));
    }
}
