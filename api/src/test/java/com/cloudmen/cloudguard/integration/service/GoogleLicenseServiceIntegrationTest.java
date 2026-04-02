package com.cloudmen.cloudguard.integration.service;

import com.cloudmen.cloudguard.dto.licenses.LicenseCacheEntry;
import com.cloudmen.cloudguard.dto.licenses.LicenseOverviewResponse;
import com.cloudmen.cloudguard.dto.licenses.LicensePageResponse;
import com.cloudmen.cloudguard.dto.licenses.LicenseType;
import com.cloudmen.cloudguard.dto.users.UserCacheEntry;
import com.cloudmen.cloudguard.service.GoogleLicenseService;
import com.cloudmen.cloudguard.service.cache.GoogleLicenseCacheService;
import com.cloudmen.cloudguard.service.cache.GoogleUsersCacheService;
import com.google.api.services.admin.directory.model.User;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

import java.util.Collections;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.Mockito.*;

@SpringBootTest
public class GoogleLicenseServiceIntegrationTest {

    @Autowired
    private GoogleLicenseService licenseService;

    @MockitoBean
    private GoogleLicenseCacheService licenseCacheService;

    @MockitoBean
    private GoogleUsersCacheService usersCacheService;

    private static final String EMAIL = "admin@cloudmen.com";

    @BeforeEach
    void setUp() {}

    @Test
    void forceRefreshCache_callsCacheService() {
        licenseService.forceRefreshCache(EMAIL);
        verify(licenseCacheService).forceRefreshCache(EMAIL);
    }

    @Test
    void getLicenses_success_returnsSortedTypesAndCalculatesStepSize() {
        LicenseType type1 = mock(LicenseType.class);
        when(type1.skuName()).thenReturn("Z_Workspace Enterprise");
        when(type1.totalAssigned()).thenReturn(15);

        LicenseType type2 = mock(LicenseType.class);
        when(type2.skuName()).thenReturn("A_Workspace Basic");
        when(type2.totalAssigned()).thenReturn(45);

        LicenseCacheEntry cacheEntry = mock(LicenseCacheEntry.class);
        when(cacheEntry.licenseTypes()).thenReturn(List.of(type1, type2));
        when(cacheEntry.inactiveUsers()).thenReturn(List.of(mock(com.cloudmen.cloudguard.dto.licenses.InactiveUser.class)));

        when(licenseCacheService.getOrFetchLicenseData(EMAIL)).thenReturn(cacheEntry);

        LicensePageResponse response = licenseService.getLicenses(EMAIL);

        assertNotNull(response);
        assertEquals(2, response.licenseTypes().size());
        assertEquals("A_Workspace Basic", response.licenseTypes().get(0).skuName());
        assertEquals("Z_Workspace Enterprise", response.licenseTypes().get(1).skuName());
        assertEquals(1, response.inactiveUsers().size());
        assertEquals(55, response.maxLicenseAmount());
        assertEquals(10, response.chartStepSize());
    }

    @Test
    void getLicenses_emptyData_returnsZeroes() {
        LicenseCacheEntry cacheEntry = mock(LicenseCacheEntry.class);
        when(cacheEntry.licenseTypes()).thenReturn(Collections.emptyList());
        when(cacheEntry.inactiveUsers()).thenReturn(Collections.emptyList());

        when(licenseCacheService.getOrFetchLicenseData(EMAIL)).thenReturn(cacheEntry);

        LicensePageResponse response = licenseService.getLicenses(EMAIL);

        assertNotNull(response);
        assertEquals(0, response.licenseTypes().size());
        assertEquals(0, response.inactiveUsers().size());
        assertEquals(2, response.maxLicenseAmount());
        assertEquals(2, response.chartStepSize());
    }

    @Test
    void getLicensesPageOverview_success_calculatesTotalsAndRiskyAccounts() {
        LicenseType type1 = mock(LicenseType.class);
        when(type1.totalAssigned()).thenReturn(10);
        LicenseType type2 = mock(LicenseType.class);
        when(type2.totalAssigned()).thenReturn(20);

        LicenseCacheEntry licenseCache = mock(LicenseCacheEntry.class);
        when(licenseCache.licenseTypes()).thenReturn(List.of(type1, type2));
        when(licenseCache.inactiveUsers()).thenReturn(List.of(
                mock(com.cloudmen.cloudguard.dto.licenses.InactiveUser.class),
                mock(com.cloudmen.cloudguard.dto.licenses.InactiveUser.class)
        ));

        when(licenseCacheService.getOrFetchLicenseData(EMAIL)).thenReturn(licenseCache);

        User suspendedUser = new User();
        suspendedUser.setSuspended(true);

        User activeUser = new User();
        activeUser.setSuspended(false);

        UserCacheEntry userCache = mock(UserCacheEntry.class);
        when(userCache.allUsers()).thenReturn(List.of(suspendedUser, activeUser));

        when(usersCacheService.getOrFetchUsersData(EMAIL)).thenReturn(userCache);

        LicenseOverviewResponse response = licenseService.getLicensesPageOverview(EMAIL);

        assertNotNull(response);
        assertEquals(30, response.totalAssigned());
        assertEquals(3, response.riskyAccounts());
        assertEquals(2, response.unusedLicenses());
    }

    @Test
    void getLicensesPageOverview_nullData_handlesSafely() {
        LicenseCacheEntry licenseCache = mock(LicenseCacheEntry.class);
        when(licenseCache.licenseTypes()).thenReturn(Collections.emptyList());
        when(licenseCache.inactiveUsers()).thenReturn(null);

        when(licenseCacheService.getOrFetchLicenseData(EMAIL)).thenReturn(licenseCache);

        UserCacheEntry userCache = mock(UserCacheEntry.class);
        when(userCache.allUsers()).thenReturn(null);

        when(usersCacheService.getOrFetchUsersData(EMAIL)).thenReturn(userCache);

        LicenseOverviewResponse response = licenseService.getLicensesPageOverview(EMAIL);

        assertNotNull(response);
        assertEquals(0, response.totalAssigned());
        assertEquals(0, response.riskyAccounts());
        assertEquals(0, response.unusedLicenses());
    }
}
