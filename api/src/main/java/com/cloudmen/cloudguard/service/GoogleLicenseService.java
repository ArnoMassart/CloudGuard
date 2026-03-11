package com.cloudmen.cloudguard.service;

import com.cloudmen.cloudguard.dto.licenses.*;
import com.cloudmen.cloudguard.service.cache.GoogleLicenseCacheService;
import com.cloudmen.cloudguard.service.cache.GoogleUsersCacheService;
import com.google.api.services.admin.directory.model.User;
import org.springframework.stereotype.Service;

import java.util.Comparator;
import java.util.List;

@Service
public class GoogleLicenseService {
    private final GoogleLicenseCacheService licenseCacheService;
    private final GoogleUsersCacheService usersCacheService;


    public GoogleLicenseService(GoogleLicenseCacheService licenseCacheService, GoogleUsersCacheService usersCacheService) {
        this.licenseCacheService = licenseCacheService;
        this.usersCacheService = usersCacheService;
    }

    public void forceRefreshCache(String loggedInEmail) {
        licenseCacheService.forceRefreshCache(loggedInEmail);
    }

    public LicensePageResponse getLicenses(String loggedInEmail) {
        LicenseCacheEntry cachedData = licenseCacheService.getOrFetchLicenseData(loggedInEmail);
        List<LicenseType> types = cachedData.licenseTypes().stream().sorted(Comparator.comparing(LicenseType::skuName)).toList();

        int maxLicenseAmount = types.stream().mapToInt(LicenseType::totalAssigned).max()
                .orElse(0);

        int stepSize = getStepSize(maxLicenseAmount);

        List<User> allDomainUsers = usersCacheService.getOrFetchUsersData(loggedInEmail).allUsers();

        int activeCount = 0;
        int inactiveCount = 0;

        if (allDomainUsers != null) {
            for (User user: allDomainUsers) {
                if (Boolean.TRUE.equals(user.getIsEnrolledIn2Sv())) {
                    activeCount++;
                } else {
                    inactiveCount++;
                }
            }
        }

        MfaStats mfaStats = new MfaStats(activeCount, inactiveCount);

        return new LicensePageResponse(
          types,
          cachedData.inactiveUsers(),
                mfaStats,
                maxLicenseAmount + stepSize,
                stepSize
        );
    }

    public LicenseOverviewResponse getLicensesPageOverview(String loggedInEmail) {
        LicenseCacheEntry cachedData = licenseCacheService.getOrFetchLicenseData(loggedInEmail);
        List<LicenseType> types = cachedData.licenseTypes();

        int totalAssigned =  types.stream().mapToInt(LicenseType::totalAssigned).sum();

        List<User> allDomainUsers = usersCacheService.getOrFetchUsersData(loggedInEmail).allUsers();

        int riskyAccounts =0;
        long unusedLicenses = cachedData.inactiveUsers() != null ? cachedData.inactiveUsers().size() : 0;
        int mfaActiveCount=0;

        if (allDomainUsers != null) {
            for (User user : allDomainUsers) {
                if (Boolean.TRUE.equals(user.getSuspended())) {
                    riskyAccounts++;
                }
                if (Boolean.TRUE.equals(user.getIsEnrolledIn2Sv())) {
                    mfaActiveCount++;
                }
            }
        }

        long totalUsers = allDomainUsers != null ? allDomainUsers.size() : 0;
        long mfaPercentage = totalUsers == 0 ? 0 : Math.round(((double) mfaActiveCount / totalUsers) * 100);

        return new LicenseOverviewResponse(
                totalAssigned,
                riskyAccounts + (int)unusedLicenses ,
                unusedLicenses,
                mfaPercentage
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
