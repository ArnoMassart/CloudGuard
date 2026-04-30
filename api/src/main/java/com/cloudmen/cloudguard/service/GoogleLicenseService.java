package com.cloudmen.cloudguard.service;

import com.cloudmen.cloudguard.dto.licenses.*;
import com.cloudmen.cloudguard.service.cache.GoogleLicenseCacheService;
import com.cloudmen.cloudguard.service.cache.GoogleUsersCacheService;
import com.google.api.services.admin.directory.model.User;
import org.springframework.stereotype.Service;

import java.util.Comparator;
import java.util.List;

/**
 * Service responsible for aggregating and analyzing Google Workspace license utilization. <p>
 *
 * This service retrieves cached license and user data to calculate the total assigned licenses, identify inactive or
 * suspended users consuming licenses (risky/unused), and prepares the data for frontend chart visualizations.
 */
@Service
public class GoogleLicenseService {
    private final GoogleLicenseCacheService licenseCacheService;
    private final GoogleUsersCacheService usersCacheService;


    public GoogleLicenseService(GoogleLicenseCacheService licenseCacheService, GoogleUsersCacheService usersCacheService) {
        this.licenseCacheService = licenseCacheService;
        this.usersCacheService = usersCacheService;
    }

    /**
     * Triggers a manual background refresh of the License cache for the specified user.
     *
     * @param loggedInEmail the email of the authenticated user
     */
    public void forceRefreshCache(String loggedInEmail) {
        licenseCacheService.forceRefreshCache(loggedInEmail);
    }

    /**
     * Retrieves detailed license distribution data formatted for frontend charting and lists. <p>
     *
     * Automatically calculates the appropriate axis step size based on the highest license count.
     *
     * @param loggedInEmail the email of the authenticated user
     * @return a {@link LicensePageResponse} containing sorted license types and chart metadata
     */
    public LicensePageResponse getLicenses(String loggedInEmail) {
        LicenseCacheEntry cachedData = licenseCacheService.getOrFetchData(loggedInEmail);
        List<LicenseType> types = cachedData.licenseTypes().stream().sorted(Comparator.comparing(LicenseType::skuName)).toList();

        int maxLicenseAmount = types.stream().mapToInt(LicenseType::totalAssigned).max()
                .orElse(0);

        int stepSize = getStepSize(maxLicenseAmount);

        return new LicensePageResponse(
          types,
          cachedData.inactiveUsers(),
                maxLicenseAmount + stepSize,
                stepSize
        );
    }

    /**
     * Retrieves a high-level overview of license utilization, highlighting potential cost-saving opportunities
     * (unused/suspended licenses).
     *
     * @param loggedInEmail the email of the authenticated user
     * @return a {@link LicenseOverviewResponse} with aggregated utilization metrics
     */
    public LicenseOverviewResponse getLicensesPageOverview(String loggedInEmail) {
        LicenseCacheEntry cachedData = licenseCacheService.getOrFetchData(loggedInEmail);
        List<LicenseType> types = cachedData.licenseTypes();

        int totalAssigned =  types.stream().mapToInt(LicenseType::totalAssigned).sum();

        List<User> allDomainUsers = usersCacheService.getOrFetchData(loggedInEmail).allUsers();

        int riskyAccounts =0;
        int unusedLicenses = cachedData.inactiveUsers() != null ? cachedData.inactiveUsers().size() : 0;

        if (allDomainUsers != null) {
            for (User user : allDomainUsers) {
                if (Boolean.TRUE.equals(user.getSuspended())) {
                    riskyAccounts++;
                }
            }
        }

        return new LicenseOverviewResponse(
                totalAssigned,
                riskyAccounts + unusedLicenses ,
                unusedLicenses
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
