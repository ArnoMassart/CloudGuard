package com.cloudmen.cloudguard.integration.service;

import com.cloudmen.cloudguard.dto.drives.SharedDriveBasicDetail;
import com.cloudmen.cloudguard.dto.drives.SharedDriveCacheEntry;
import com.cloudmen.cloudguard.dto.drives.SharedDriveOverviewResponse;
import com.cloudmen.cloudguard.dto.drives.SharedDrivePageResponse;
import com.cloudmen.cloudguard.service.GoogleSharedDriveService;
import com.cloudmen.cloudguard.service.UserService;
import com.cloudmen.cloudguard.service.cache.GoogleSharedDriveCacheService;
import com.google.api.client.util.DateTime;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.MessageSource;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

import java.util.Collections;
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

@SpringBootTest(classes = {GoogleSharedDriveService.class})
public class GoogleSharedDriveServiceIntegrationTest {

    @Autowired
    private GoogleSharedDriveService googleSharedDriveService;

    @MockitoBean
    private GoogleSharedDriveCacheService sharedDriveCacheService;

    @MockitoBean
    private MessageSource messageSource;

    private static final String EMAIL = "admin@cloudmen.com";

    @BeforeEach
    void setUp() {
        when(messageSource.getMessage(anyString(), any(), any(Locale.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));
    }

    @Test
    void forceRefreshCache_callsCacheService() {
        googleSharedDriveService.forceRefreshCache(EMAIL);
        verify(sharedDriveCacheService).forceRefreshCache(EMAIL);
    }

    @Test
    void getDrivesPageOverview_success_calculatesCorrectScoresAndWarnings() {
        DateTime now = new DateTime(System.currentTimeMillis());

        SharedDriveCacheEntry cacheEntry = getSharedDriveCacheEntry(now);
        when(sharedDriveCacheService.getOrFetchDriveData(EMAIL)).thenReturn(cacheEntry);

        SharedDriveOverviewResponse response = googleSharedDriveService.getDrivesPageOverview(EMAIL, Set.of());

        assertNotNull(response);
        assertEquals(3, response.totalDrives());
        assertEquals(1, response.orphanDrives());
        assertEquals(1, response.totalHighRisk());
        assertEquals(2, response.totalExternalMembersCount());

        assertEquals(1, response.notOnlyDomainUsersAllowedCount());
        assertEquals(2, response.notOnlyMembersCanAccessCount());

        assertTrue(response.securityScore() < 100);

        assertNotNull(response.securityScoreBreakdown());
        assertEquals(6, response.securityScoreBreakdown().factors().size());

        assertNotNull(response.warnings());
        assertTrue(response.warnings().hasWarnings());
    }

    private static SharedDriveCacheEntry getSharedDriveCacheEntry(DateTime now) {
        SharedDriveBasicDetail safeDrive = new SharedDriveBasicDetail(
                "d1", "Finance Drive", 10, 0, 2, now, "Just now", true, true, "low"
        );
        SharedDriveBasicDetail mediumDrive = new SharedDriveBasicDetail(
                "d2", "Marketing Drive", 15, 5, 1, now, "Just now", true, false, "middle"
        );
        SharedDriveBasicDetail riskyOrphanDrive = new SharedDriveBasicDetail(
                "d3", "External Project", 20, 10, 0, now, "Just now", false, false, "high"
        );

        SharedDriveCacheEntry cacheEntry = new SharedDriveCacheEntry(List.of(safeDrive, mediumDrive, riskyOrphanDrive), now.getValue());
        return cacheEntry;
    }

    @Test
    void getDrivesPageOverview_withDisabledPreferences_adjustsScores() {
        DateTime now = new DateTime(System.currentTimeMillis());

        SharedDriveBasicDetail riskyOrphanDrive = new SharedDriveBasicDetail(
                "d1", "External Project", 20, 10, 0, now, "Just now", false, false, "high"
        );

        SharedDriveCacheEntry cacheEntry = new SharedDriveCacheEntry(List.of(riskyOrphanDrive), now.getValue());
        when(sharedDriveCacheService.getOrFetchDriveData(EMAIL)).thenReturn(cacheEntry);

        Set<String> disabledKeys = Set.of(
                "shared-drives.orphan",
                "shared-drives.outsideDomain",
                "shared-drives.nonMemberAccess",
                "shared-drives.external"
        );

        SharedDriveOverviewResponse response = googleSharedDriveService.getDrivesPageOverview(EMAIL, disabledKeys);

        assertNotNull(response);
        assertEquals(100, response.securityScore());

        assertTrue(response.securityScoreBreakdown().factors().get(0).muted());
        assertTrue(response.securityScoreBreakdown().factors().get(3).muted());
        assertTrue(response.securityScoreBreakdown().factors().get(4).muted());
        assertTrue(response.securityScoreBreakdown().factors().get(5).muted());

        assertFalse(response.warnings().hasWarnings());
    }

    @Test
    void getDrivesPageOverview_emptyDrives_returnsPerfectScores() {
        SharedDriveCacheEntry cacheEntry = new SharedDriveCacheEntry(Collections.emptyList(), System.currentTimeMillis());
        when(sharedDriveCacheService.getOrFetchDriveData(EMAIL)).thenReturn(cacheEntry);

        SharedDriveOverviewResponse response = googleSharedDriveService.getDrivesPageOverview(EMAIL, Set.of());

        assertNotNull(response);
        assertEquals(0, response.totalDrives());
        assertEquals(100, response.securityScore());
        assertEquals(100, response.securityScoreBreakdown().totalScore());
        assertEquals("perfect", response.securityScoreBreakdown().status());
    }

    @Test
    void getSharedDrivesPaged_noFilters_returnsPagedResults() {
        DateTime now = new DateTime(System.currentTimeMillis());
        SharedDriveBasicDetail drive1 = new SharedDriveBasicDetail("d1", "Alpha", 5, 0, 1, now, "", true, true, "low");
        SharedDriveBasicDetail drive2 = new SharedDriveBasicDetail("d2", "Beta", 5, 0, 1, now, "", true, true, "low");
        SharedDriveBasicDetail drive3 = new SharedDriveBasicDetail("d3", "Gamma", 5, 0, 1, now, "", true, true, "low");

        SharedDriveCacheEntry cacheEntry = new SharedDriveCacheEntry(List.of(drive1, drive2, drive3), now.getValue());
        when(sharedDriveCacheService.getOrFetchDriveData(EMAIL)).thenReturn(cacheEntry);

        SharedDrivePageResponse page1 = googleSharedDriveService.getSharedDrivesPaged(EMAIL, null, 2, null);
        assertNotNull(page1);
        assertEquals(2, page1.drives().size());
        assertEquals("d1", page1.drives().get(0).id());
        assertEquals("2", page1.nextPageToken());

        SharedDrivePageResponse page2 = googleSharedDriveService.getSharedDrivesPaged(EMAIL, "2", 2, null);
        assertNotNull(page2);
        assertEquals(1, page2.drives().size());
        assertEquals("d3", page2.drives().get(0).id());
        assertNull(page2.nextPageToken());
    }

    @Test
    void getSharedDrivesPaged_withQueryFilter_returnsFilteredResults() {
        DateTime now = new DateTime(System.currentTimeMillis());
        SharedDriveBasicDetail drive1 = new SharedDriveBasicDetail("d1", "Alpha Project", 5, 0, 1, now, "", true, true, "low");
        SharedDriveBasicDetail drive2 = new SharedDriveBasicDetail("d2", "Beta Test", 5, 0, 1, now, "", true, true, "low");

        SharedDriveCacheEntry cacheEntry = new SharedDriveCacheEntry(List.of(drive1, drive2), now.getValue());
        when(sharedDriveCacheService.getOrFetchDriveData(EMAIL)).thenReturn(cacheEntry);

        SharedDrivePageResponse response = googleSharedDriveService.getSharedDrivesPaged(EMAIL, "1", 10, "project");

        assertNotNull(response);
        assertEquals(1, response.drives().size());
        assertEquals("d1", response.drives().get(0).id());
        assertNull(response.nextPageToken());
    }
}