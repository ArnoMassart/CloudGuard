package com.cloudmen.cloudguard.unit.service;

import com.cloudmen.cloudguard.service.GoogleLicenseService;
import com.cloudmen.cloudguard.service.cache.GoogleLicenseCacheService;
import com.cloudmen.cloudguard.service.cache.GoogleUsersCacheService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static com.cloudmen.cloudguard.unit.helper.GlobalTestHelper.*;
import static com.cloudmen.cloudguard.unit.helper.GoogleLicenseTestHelper.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
public class GoogleLicenseServiceTest {

    @Mock
    private GoogleLicenseCacheService licenseCacheService;

    @Mock
    private GoogleUsersCacheService usersCacheService;

    private GoogleLicenseService service;

    @BeforeEach
    void setUp() {
        service = new GoogleLicenseService(licenseCacheService, usersCacheService);
    }

    @Test
    void forceRefreshCache_delegatesToCacheService() {
        service.forceRefreshCache(ADMIN);
        verify(licenseCacheService).forceRefreshCache(ADMIN);
    }

    @Test
    void getLicenses_emptyCache_returnsZeroesAndStepSizeTwo() {
        mockLicenseCacheEntry(licenseCacheService, List.of(), List.of());

        var response = service.getLicenses(ADMIN);

        assertTrue(response.licenseTypes().isEmpty());
        assertTrue(response.inactiveUsers().isEmpty());

        assertEquals(2, response.chartStepSize());
        assertEquals(2, response.maxLicenseAmount());
    }

    @Test
    void getLicenses_sortsLicenseTypesAlphabetically_bySkuName() {
        var types = List.of(
                createLicenseType("3", "Zeta License", 10),
                createLicenseType("1", "Alpha License", 5),
                createLicenseType("2", "Beta License", 15)
        );

        mockLicenseCacheEntry(licenseCacheService, types, List.of());

        var response = service.getLicenses(ADMIN);

        assertEquals(3, response.licenseTypes().size());
        assertEquals("Alpha License", response.licenseTypes().get(0).skuName());
        assertEquals("Beta License", response.licenseTypes().get(1).skuName());
        assertEquals("Zeta License", response.licenseTypes().get(2).skuName());
    }

    @Test
    void getLicenses_calculatesStepSizeCorrectly_basedOnMaxAssigned() {
        // Test 1: Max = 8 (<= 10) -> Expected Step = 2
        mockLicenseCacheEntry(licenseCacheService, List.of(createLicenseType("1", "A", 8)), List.of());
        assertEquals(2, service.getLicenses(ADMIN).chartStepSize());

        // Test 2: Max = 20 (<= 25) -> Expected Step = 5
        mockLicenseCacheEntry(licenseCacheService, List.of(createLicenseType("1", "A", 20)), List.of());
        assertEquals(5, service.getLicenses(ADMIN).chartStepSize());

        // Test 3: Max = 45 (<= 50) -> Expected Step = 10
        mockLicenseCacheEntry(licenseCacheService, List.of(createLicenseType("1", "A", 45)), List.of());
        assertEquals(10, service.getLicenses(ADMIN).chartStepSize());

        // Test 4: Max = 90 (<= 100) -> Expected Step = 20
        mockLicenseCacheEntry(licenseCacheService, List.of(createLicenseType("1", "A", 90)), List.of());
        assertEquals(20, service.getLicenses(ADMIN).chartStepSize());

        // Test 5: Max = 120 (> 100) -> Math.ceil(120 / 50.0) * 10 = Math.ceil(2.4) * 10 = 30
        mockLicenseCacheEntry(licenseCacheService, List.of(createLicenseType("1", "A", 120)), List.of());
        assertEquals(30, service.getLicenses(ADMIN).chartStepSize());
    }

    @Test
    void getLicensesPageOverview_noData_returnsZeroes() {
        mockLicenseCacheEntry(licenseCacheService, List.of(), List.of());
        mockUsersCacheEntry(usersCacheService, List.of());

        var overview = service.getLicensesPageOverview(ADMIN);

        assertEquals(0, overview.totalAssigned());
        assertEquals(0, overview.riskyAccounts());
        assertEquals(0, overview.unusedLicenses());
    }

    @Test
    void getLicensesPageOverview_handlesNullListsGracefully() {
        mockLicenseCacheEntry(licenseCacheService, List.of(), null);
        mockUsersCacheEntry(usersCacheService, null);

        var overview = service.getLicensesPageOverview(ADMIN);

        assertEquals(0, overview.totalAssigned());
        assertEquals(0, overview.riskyAccounts());
        assertEquals(0, overview.unusedLicenses());
    }

    @Test
    void getLicensesPageOverview_calculatesTotalsAndRisksCorrectly() {
        var types = List.of(
                createLicenseType("1", "Basic", 5),
                createLicenseType("2", "Plus", 10)
        );
        // 2 Inactive users
        var inactiveUsers = List.of(
                createInactiveUser("inactive1@x.com"),
                createInactiveUser("inactive2@x.com")
        );

        mockLicenseCacheEntry(licenseCacheService, types, inactiveUsers);

        var users = List.of(
                createUser("suspended@x.com", true),
                createUser("active1@x.com", false),
                createUser("active2@x.com", false)
        );

        mockUsersCacheEntry(usersCacheService, users);

        var overview = service.getLicensesPageOverview(ADMIN);

        assertEquals(15, overview.totalAssigned());
        assertEquals(2, overview.unusedLicenses());
        assertEquals(3, overview.riskyAccounts());
    }
}
