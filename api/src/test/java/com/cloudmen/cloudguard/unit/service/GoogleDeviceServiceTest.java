package com.cloudmen.cloudguard.unit.service;

import com.cloudmen.cloudguard.dto.password.SecurityScoreFactorDto;
import com.cloudmen.cloudguard.service.GoogleDeviceService;
import com.cloudmen.cloudguard.service.cache.GoogleDeviceCacheService;
import com.google.api.services.admin.directory.model.MobileDevice;
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
import static com.cloudmen.cloudguard.unit.helper.GoogleDeviceTestHelper.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
public class GoogleDeviceServiceTest {
    @Mock
    private GoogleDeviceCacheService deviceCacheService;

    private GoogleDeviceService service;

    @BeforeEach
    void setUp() {
        service = new GoogleDeviceService(deviceCacheService);
    }

    @AfterEach
    void tearDown() {
        LocaleContextHolder.resetLocaleContext();
    }

    @Test
    void forceRefreshCache_delegatesToCacheService() {
        service.forceRefreshCache(ADMIN);
        verify(deviceCacheService).forceRefreshCache(ADMIN);
    }

    @Test
    void getDevicesPaged_emptyCache_returnsEmptyPageAndNoNextToken() {
        mockCacheEntry(deviceCacheService, List.of(), List.of(), List.of());

        var page = service.getDevicesPaged(ADMIN, null, 10, "ALL", "all", Set.of());

        assertTrue(page.devices().isEmpty());
        assertNull(page.nextPageToken());
    }

    @Test
    void getDevicesPaged_mapsAllThreeDeviceTypesCorrectly() {
        var mobile = createMobileDevice("1", "m@x.com", "Android 14", "APPROVED", "PASSWORD_SET", "ENCRYPTED", "UNCOMPROMISED", daysAgo(1));
        var chrome = createChromeOsDevice("2", "c@x.com", "114.0", "ACTIVE", daysAgo(1));
        var endpoint = createEndpointWrapper("devices/3", "MAC_OS", "e@x.com", "12.0", "ENCRYPTED", "UNCOMPROMISED", "2023-10-01T12:00:00Z");

        mockCacheEntry(deviceCacheService, List.of(mobile), List.of(chrome), List.of(endpoint));

        var page = service.getDevicesPaged(ADMIN, null, 10, "ALL", "all", Set.of());

        assertEquals(3, page.devices().size());
        assertTrue(page.devices().stream().anyMatch(d -> d.os().contains("Android")));
        assertTrue(page.devices().stream().anyMatch(d -> d.os().contains("ChromeOS")));
        assertTrue(page.devices().stream().anyMatch(d -> d.deviceType().equals("MAC")));
    }

    @Test
    void getDevicesPaged_filtersByStatusAndType() {
        var mobile1 = createMobileDevice("1", "m1@x.com", "Android 14", "APPROVED", "PASSWORD_SET", "ENCRYPTED", "UNCOMPROMISED", daysAgo(1));
        var mobile2 = createMobileDevice("2", "m2@x.com", "iOS 17", "BLOCKED", "PASSWORD_SET", "ENCRYPTED", "UNCOMPROMISED", daysAgo(1));
        var chrome = createChromeOsDevice("3", "c@x.com", "114.0", "ACTIVE", daysAgo(1));

        mockCacheEntry(deviceCacheService, List.of(mobile1, mobile2), List.of(chrome), List.of());

        var pageStatus = service.getDevicesPaged(ADMIN, null, 10, "APPROVED", "all", Set.of());
        assertEquals(2, pageStatus.devices().size());

        var pageType = service.getDevicesPaged(ADMIN, null, 10, "ALL", "ios", Set.of());
        assertEquals(1, pageType.devices().size());
        assertEquals("iOS 17", pageType.devices().get(0).os());
    }

    @Test
    void getDevicesPaged_paginationLogic() {
        List<MobileDevice> mobiles = new ArrayList<>();
        for (int i = 0; i < 5; i++) {
            mobiles.add(createMobileDevice(String.valueOf(i), "m" + i + "@x.com", "Android", "APPROVED", "x", "x", "x", daysAgo(1)));
        }
        mockCacheEntry(deviceCacheService, mobiles, List.of(), List.of());

        var page1 = service.getDevicesPaged(ADMIN, "1", 2, "ALL", "all", Set.of());
        assertEquals(2, page1.devices().size());
        assertEquals("2", page1.nextPageToken());

        var page3 = service.getDevicesPaged(ADMIN, "3", 2, "ALL", "all", Set.of());
        assertEquals(1, page3.devices().size());
        assertNull(page3.nextPageToken());
    }

    @Test
    void getUniqueDeviceTypes_normalizesAndSortsOSNames() {
        var m1 = createMobileDevice("1", "a@x.com", "Android 14", "APPROVED", "x", "x", "x", daysAgo(1));
        var m2 = createMobileDevice("2", "b@x.com", "iOS 17.1", "APPROVED", "x", "x", "x", daysAgo(1));
        var m3 = createMobileDevice("3", "c@x.com", "Windows 11", "APPROVED", "x", "x", "x", daysAgo(1));
        var c1 = createChromeOsDevice("4", "d@x.com", "114.0", "ACTIVE", daysAgo(1));

        mockCacheEntry(deviceCacheService, List.of(m1, m2, m3), List.of(c1), List.of());

        List<String> types = service.getUniqueDeviceTypes(ADMIN);

        assertEquals(4, types.size());
        assertEquals("Android", types.get(0));
        assertEquals("ChromeOS", types.get(1));
        assertEquals("iOS", types.get(2));
        assertEquals("Windows", types.get(3));
    }

    @Test
    void getDevicesPageOverview_calculatesCountsAndScoresCorrectly() {
        var perfectMobile = createMobileDevice("1", "a@x.com", "Android 14", "APPROVED", "PASSWORD_SET", "ENCRYPTED", "UNCOMPROMISED", daysAgo(1));
        // 2. Bad Mobile Device (Missing all security features -> Score 0)
        var badMobile = createMobileDevice("2", "b@x.com", "Android 9", "BLOCKED", "NONE", "UNENCRYPTED", "COMPROMISED", daysAgo(10));
        // 3. Perfect ChromeOS (Mapped to 100 automatically)
        var chrome = createChromeOsDevice("3", "c@x.com", "114.0", "ACTIVE", daysAgo(1));

        mockCacheEntry(deviceCacheService, List.of(perfectMobile, badMobile), List.of(chrome), List.of());

        var overview = service.getDevicesPageOverview(ADMIN, Set.of());

        assertEquals(3, overview.totalDevices());
        assertEquals(2, overview.totalApprovedDevices());
        assertEquals(1, overview.totalNonCompliant());

        assertEquals(1, overview.lockScreenCount());
        assertEquals(1, overview.encryptionCount());
        assertEquals(1, overview.osVersionCount());
        assertEquals(1, overview.integrityCount());

        assertEquals(67, overview.securityScore());

        assertTrue(overview.warnings().hasWarnings());
        assertTrue(overview.warnings().items().get("lockScreenWarning"));
    }

    @Test
    void getDevicesPageOverview_withDisabledPreferences_forcesScore100AndMutedBreakdown() {
        var badMobile = createMobileDevice("2", "b@x.com", "Android 9", "BLOCKED", "NONE", "UNENCRYPTED", "COMPROMISED", daysAgo(10));

        mockCacheEntry(deviceCacheService, List.of(badMobile), List.of(), List.of());

        Set<String> disabledPrefs = Set.of(
                "mobile-devices:lockscreen",
                "mobile-devices:encryption",
                "mobile-devices:osVersion",
                "mobile-devices:integrity"
        );

        var overview = service.getDevicesPageOverview(ADMIN, disabledPrefs);

        assertEquals(100, overview.securityScore());

        assertTrue(overview.securityScoreBreakdown().factors().stream().allMatch(SecurityScoreFactorDto::muted));

        assertNotNull(overview.warnings());
        assertFalse(overview.warnings().hasWarnings());
    }
}
