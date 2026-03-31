package com.cloudmen.cloudguard.service;

import com.cloudmen.cloudguard.dto.drives.SharedDriveBasicDetail;
import com.cloudmen.cloudguard.dto.password.SecurityScoreFactorDto;
import com.cloudmen.cloudguard.service.cache.GoogleSharedDriveCacheService;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.i18n.LocaleContextHolder;
import org.springframework.context.support.ResourceBundleMessageSource;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import static com.cloudmen.cloudguard.utility.GlobalTestHelper.*;
import static com.cloudmen.cloudguard.utility.GoogleSharedDriveTestHelper.createDrive;
import static com.cloudmen.cloudguard.utility.GoogleSharedDriveTestHelper.mockCacheEntry;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
public class GoogleSharedDriveServiceTest {

    @Mock
    private GoogleSharedDriveCacheService sharedDriveCacheService;

    private GoogleSharedDriveService service;

    @BeforeEach
    void setUp() {
        service = new GoogleSharedDriveService(sharedDriveCacheService, getMessageSource());
    }

    @AfterEach
    void tearDown() {
        LocaleContextHolder.resetLocaleContext();
    }

    @Test
    void forceRefreshCache_delegatesToCacheService() {
        service.forceRefreshCache(ADMIN);
        verify(sharedDriveCacheService).forceRefreshCache(ADMIN);
    }

    @Test
    void getSharedDrivesPaged_emptyCache_returnsEmptyPageAndNoNextToken() {
        mockCacheEntry(sharedDriveCacheService, List.of());

        var page = service.getSharedDrivesPaged(ADMIN, null, 10, null);

        assertTrue(page.drives().isEmpty());
        assertNull(page.nextPageToken());
    }

    @Test
    void getSharedDrivesPaged_queryFiltersByName() {
        var drives = List.of(
                createDrive("1", "Marketing Assets", 10, 0, 2, true, true, "low", daysAgo(10)),
                createDrive("2", "Finance Confidential", 5, 0, 1, true, true, "low", daysAgo(10)),
                createDrive("3", "Global Marketing", 50, 5, 3, false, false, "high", daysAgo(10))
        );

        mockCacheEntry(sharedDriveCacheService, drives);

        var page = service.getSharedDrivesPaged(ADMIN, null, 10, "marketing");

        assertEquals(2, page.drives().size());
        assertTrue(page.drives().stream().anyMatch(d -> d.name().equals("Marketing Assets")));
        assertTrue(page.drives().stream().anyMatch(d -> d.name().equals("Global Marketing")));
    }

    @Test
    void getSharedDrivesPaged_blankQuery_returnsAll(){
        var drives = List.of(
                createDrive("1", "Alpha", 10, 0, 2, true, true, "low", daysAgo(10)),
                createDrive("2", "Beta", 5, 0, 1, true, true, "low", daysAgo(10))
        );

        mockCacheEntry(sharedDriveCacheService, drives);

        var page = service.getSharedDrivesPaged(ADMIN, "1", 10, " ");

        assertEquals(2, page.drives().size());
    }

    @Test
    void getSharedDrivesPaged_paginationLogic(){
        var drives = new ArrayList<SharedDriveBasicDetail>();
        for (int i = 0; i < 5; i++) {
            drives.add(createDrive(String.valueOf(i), "Drive " + i, 10, 0, 1, true, true, "low", daysAgo(10)));
        }

        mockCacheEntry(sharedDriveCacheService, drives);

        var page1 = service.getSharedDrivesPaged(ADMIN, "1", 2, null);
        assertEquals(2, page1.drives().size());
        assertEquals("Drive 0", page1.drives().get(0).name());
        assertEquals("2", page1.nextPageToken());

        // Page 2
        var page2 = service.getSharedDrivesPaged(ADMIN, "2", 2, null);
        assertEquals(2, page2.drives().size());
        assertEquals("Drive 2", page2.drives().get(0).name());
        assertEquals("3", page2.nextPageToken());

        // Page 3 (Last page)
        var page3 = service.getSharedDrivesPaged(ADMIN, "3", 2, null);
        assertEquals(1, page3.drives().size());
        assertEquals("Drive 4", page3.drives().get(0).name());
        assertNull(page3.nextPageToken());
    }

    @Test
    void getDrivesPageOverview_noDrives_perfectScore() {
        mockCacheEntry(sharedDriveCacheService, List.of());

        var overview = service.getDrivesPageOverview(ADMIN, null);

        assertEquals(0, overview.totalDrives());
        assertEquals(100, overview.securityScore());
        assertEquals("perfect", overview.securityScoreBreakdown().status());
        assertFalse(overview.warnings().hasWarnings());
    }

    @Test
    void getDrivesPageOverview_calculatesCountsAndScores() {
        var drives = List.of(
                // 1. Perfect Drive
                createDrive("1", "Perfect", 10, 0, 2, true, true, "low", daysAgo(10)),
                // 2. High risk, Orphan, External access
                createDrive("2", "Danger", 50, 5, 0, false, false, "high", daysAgo(10)),
                // 3. Medium risk
                createDrive("3", "Warning", 20, 0, 1, true, true, "middle", daysAgo(10))
        );

        mockCacheEntry(sharedDriveCacheService, drives);

        var overview = service.getDrivesPageOverview(ADMIN, Set.of());

        assertEquals(3, overview.totalDrives());
        assertEquals(1, overview.orphanDrives());
        assertEquals(1, overview.totalHighRisk());
        assertEquals(1, overview.totalExternalMembersCount());

        assertTrue(overview.warnings().hasWarnings());
        assertTrue(overview.warnings().items().get("orphanDrivesWarning"));
        assertTrue(overview.warnings().items().get("notOnlyDomainUsersAllowedWarning"));
        assertTrue(overview.warnings().items().get("notOnlyMembersCanAccessWarning"));
        assertTrue(overview.warnings().items().get("externalMembersWarning"));

        // Score should be reduced
        assertTrue(overview.securityScore() < 100);
        assertNotEquals("perfect", overview.securityScoreBreakdown().status());
    }

    @Test
    void getDrivesPageOverview_withDisabledPreferences_forcesScore100AndMutedBreakdown() {
        var drives = List.of(
                // A drive that violates EVERYTHING
                createDrive("1", "Bad Drive", 50, 5, 0, false, false, "high", daysAgo(10))
        );
        mockCacheEntry(sharedDriveCacheService, drives);

        // Disable ALL sections for Shared Drives
        Set<String> disabledPrefs = Set.of(
                "shared-drives:orphan",
                "shared-drives:outsideDomain",
                "shared-drives:nonMemberAccess",
                "shared-drives:external"
        );

        var overview = service.getDrivesPageOverview(ADMIN, disabledPrefs);

        // Counts should still be calculated
        assertEquals(1, overview.totalDrives());
        assertEquals(1, overview.orphanDrives());
        assertEquals(1, overview.totalHighRisk());

        // BUT Score is forced to 100 because all violations are ignored
        assertEquals(100, overview.securityScore());

        // Breakdown factors should all be muted
        assertTrue(overview.securityScoreBreakdown().factors().stream()
                .allMatch(SecurityScoreFactorDto::muted));

        // Warnings should be suppressed completely
        assertNotNull(overview.warnings());
        assertFalse(overview.warnings().hasWarnings());
    }
}
