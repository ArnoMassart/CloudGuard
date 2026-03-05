package com.cloudmen.cloudguard.service;

import com.cloudmen.cloudguard.dto.licenses.LicenseCacheEntry;
import com.cloudmen.cloudguard.dto.licenses.LicenseOverviewResponse;
import com.cloudmen.cloudguard.dto.licenses.LicensePageResponse;
import com.cloudmen.cloudguard.dto.licenses.LicenseType;
import com.cloudmen.cloudguard.service.cache.GoogleLicenseCacheService;
import org.springframework.stereotype.Service;

import java.util.Comparator;
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
        List<LicenseType> types = cachedData.licenseTypes().stream().sorted(Comparator.comparing(LicenseType::skuName)).toList();

        int maxLicenseAmount = types.stream().mapToInt(LicenseType::totalPurchased).max()
                .orElse(0);

        int stepSize = getStepSize(maxLicenseAmount);

        return new LicensePageResponse(
          types,
          cachedData.inactiveUsers(),
                maxLicenseAmount,
                stepSize
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

    private int getStepSize(int maxAmount) {
        if (maxAmount <= 10)
            return 2;
        else if (maxAmount <= 25)
            return 5;
        else if (maxAmount <= 50)
            return 10;
        else if (maxAmount <= 100)
            return 20;
        else {
            return (int) Math.ceil(maxAmount / 5.0 / 10.0) * 10;
        }
    }
}
