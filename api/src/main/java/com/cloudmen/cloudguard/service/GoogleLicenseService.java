package com.cloudmen.cloudguard.service;

import com.cloudmen.cloudguard.dto.licenses.LicenseCacheEntry;
import com.cloudmen.cloudguard.dto.licenses.LicenseOverviewResponse;
import com.cloudmen.cloudguard.dto.licenses.LicensePageResponse;
import com.cloudmen.cloudguard.dto.licenses.LicenseType;
import com.cloudmen.cloudguard.service.cache.GoogleLicenseCacheService;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class GoogleLicenseService {
    private final GoogleLicenseCacheService licenseCacheService;

    public GoogleLicenseService(GoogleLicenseCacheService licenseCacheService) {
        this.licenseCacheService = licenseCacheService;
    }

    public void forceRefreshCache(String loggedInEmail) {
        licenseCacheService.forceRefreshCache(loggedInEmail);
    }

    public LicensePageResponse getLicenses(String loggedInEmail) {
        LicenseCacheEntry cachedData = licenseCacheService.getOrFetchLicenseData(loggedInEmail);
        List<LicenseType> types = cachedData.licenseTypes();

        int maxLicenseAmount = types.stream().mapToInt(LicenseType::totalPurchased).max()
                .orElse(0);

        return new LicensePageResponse(
          types,
          cachedData.inactiveUsers(),
                maxLicenseAmount
        );
    }

    public LicenseOverviewResponse getLicensesPageOverview(String loggedInEmail) {
        LicenseCacheEntry cachedData = licenseCacheService.getOrFetchLicenseData(loggedInEmail);
        List<LicenseType> types = cachedData.licenseTypes();

        int totalPurchased = types.stream().mapToInt(LicenseType::totalPurchased).sum();
        int totalAssigned =  types.stream().mapToInt(LicenseType::totalAssigned).sum();

        int usagePercentage = totalPurchased == 0 ? 0 : (int) Math.round(((double) totalAssigned / totalPurchased) *100);

        return new LicenseOverviewResponse(
                totalPurchased,
                usagePercentage
        );
    }
}
