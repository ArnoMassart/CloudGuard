package com.cloudmen.cloudguard.integration.service;

import com.cloudmen.cloudguard.dto.devices.DeviceCacheEntry;
import com.cloudmen.cloudguard.dto.devices.DeviceOverviewResponse;
import com.cloudmen.cloudguard.dto.devices.DevicePageResponse;
import com.cloudmen.cloudguard.service.GoogleDeviceService;
import com.cloudmen.cloudguard.service.cache.GoogleDeviceCacheService;
import com.google.api.services.admin.directory.model.ChromeOsDevice;
import com.google.api.services.admin.directory.model.MobileDevice;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.MessageSource;
import org.springframework.context.i18n.LocaleContextHolder;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@SpringBootTest(classes = {GoogleDeviceService.class})
public class GoogleDeviceServiceIT {

    @Autowired
    private GoogleDeviceService googleDeviceService;

    @MockitoBean
    private GoogleDeviceCacheService deviceCacheService;

    @MockitoBean(name = "messageSource")
    private MessageSource messageSource;

    private static final String EMAIL = "admin@cloudmen.com";

    @BeforeEach
    void setUp() {
        LocaleContextHolder.setLocale(Locale.ENGLISH);

        when(messageSource.getMessage(anyString(), nullable(Object[].class), any(Locale.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        MobileDevice compliantMobile = new MobileDevice();
        compliantMobile.setResourceId("mob-1");
        compliantMobile.setName(List.of("user1@cloudmen.com"));
        compliantMobile.setOs("Android 14");
        compliantMobile.setStatus("APPROVED");
        compliantMobile.setDevicePasswordStatus("PASSWORD_SET");
        compliantMobile.setEncryptionStatus("ENCRYPTED");
        compliantMobile.setDeviceCompromisedStatus("UNCOMPROMISED");

        MobileDevice nonCompliantMobile = new MobileDevice();
        nonCompliantMobile.setResourceId("mob-2");
        nonCompliantMobile.setName(List.of("user2@cloudmen.com"));
        nonCompliantMobile.setOs("iOS 12");
        nonCompliantMobile.setStatus("APPROVED");
        nonCompliantMobile.setDevicePasswordStatus("PASSWORD_NOT_SET");
        nonCompliantMobile.setEncryptionStatus("UNENCRYPTED");
        nonCompliantMobile.setDeviceCompromisedStatus("COMPROMISED");

        ChromeOsDevice chromeOsDevice = new ChromeOsDevice();
        chromeOsDevice.setDeviceId("chr-1");
        chromeOsDevice.setOsVersion("114.0");
        chromeOsDevice.setStatus("ACTIVE");

        DeviceCacheEntry cacheMock = mock(DeviceCacheEntry.class);
        when(cacheMock.mobileDevices()).thenReturn(List.of(compliantMobile, nonCompliantMobile));
        when(cacheMock.chromeOsDevices()).thenReturn(List.of(chromeOsDevice));
        when(cacheMock.endpointDevices()).thenReturn(Collections.emptyList());

        when(deviceCacheService.getOrFetchDeviceData(EMAIL)).thenReturn(cacheMock);
    }

    @Test
    void forceRefreshCache_callsCacheService() {
        googleDeviceService.forceRefreshCache(EMAIL);
        verify(deviceCacheService).forceRefreshCache(EMAIL);
    }

    @Test
    void getDevicesPageOverview_success_calculatesCorrectScores() {
        DeviceOverviewResponse response = googleDeviceService.getDevicesPageOverview(EMAIL);

        assertNotNull(response);
        assertEquals(3, response.totalDevices());
        assertEquals(1, response.totalNonCompliant());
        assertEquals(3, response.totalApprovedDevices());
        assertEquals(1, response.lockScreenCount());
        assertEquals(1, response.encryptionCount());
        assertEquals(1, response.integrityCount());
        assertEquals(1, response.osVersionCount());
        assertEquals(67, response.securityScore());

        assertNotNull(response.securityScoreBreakdown());
        assertEquals(4, response.securityScoreBreakdown().factors().size());
        assertEquals("average", response.securityScoreBreakdown().status());

        assertNotNull(response.warnings());
    }

    @Test
    void getDevicesPageOverview_withDisabledPreferences_adjustsScores() {
        Set<String> disabledKeys = Set.of("mobile-devices:lockscreen", "mobile-devices:encryption");

        DeviceOverviewResponse response = googleDeviceService.getDevicesPageOverview(EMAIL, disabledKeys);

        assertNotNull(response);
        assertEquals(67, response.securityScore());
        assertEquals(0, response.lockScreenCount());
        assertEquals(0, response.encryptionCount());
        assertEquals(1, response.osVersionCount());

        assertTrue(response.securityScoreBreakdown().factors().get(0).muted());
        assertTrue(response.securityScoreBreakdown().factors().get(1).muted());
    }

    @Test
    void getUniqueDeviceTypes_returnsSortedNormalizedList() {
        List<String> types = googleDeviceService.getUniqueDeviceTypes(EMAIL);

        assertNotNull(types);
        assertEquals(3, types.size());
        assertEquals("Android", types.get(0));
        assertEquals("ChromeOS", types.get(1));
        assertEquals("iOS", types.get(2));
    }

    @Test
    void getDevicesPaged_noFilters_returnsPagedResults() {
        DevicePageResponse page1 = googleDeviceService.getDevicesPaged(EMAIL, null, 2, "ALL", "all", Set.of());
        assertNotNull(page1);
        assertEquals(2, page1.devices().size());
        assertEquals("2", page1.nextPageToken());

        DevicePageResponse page2 = googleDeviceService.getDevicesPaged(EMAIL, "2", 2, "ALL", "all", Set.of());
        assertNotNull(page2);
        assertEquals(1, page2.devices().size());
        assertNull(page2.nextPageToken());
    }

    @Test
    void getDevicesPaged_withStatusFilter_returnsFilteredResults() {
        DevicePageResponse response = googleDeviceService.getDevicesPaged(EMAIL, null, 10, "APPROVED", "all", Set.of());

        assertNotNull(response);
        assertEquals(3, response.devices().size());
    }

    @Test
    void getDevicesPaged_withOsFilter_returnsFilteredResults() {
        DevicePageResponse response = googleDeviceService.getDevicesPaged(EMAIL, null, 10, "ALL", "Android", Set.of());

        assertNotNull(response);
        assertEquals(1, response.devices().size());
        assertEquals("Android 14", response.devices().get(0).os());
    }
}
