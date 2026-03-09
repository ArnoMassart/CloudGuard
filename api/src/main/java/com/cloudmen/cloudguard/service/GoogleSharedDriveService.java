package com.cloudmen.cloudguard.service;

import com.cloudmen.cloudguard.dto.drives.*;
import com.cloudmen.cloudguard.service.cache.GoogleSharedDriveCacheService;
import com.cloudmen.cloudguard.utility.GoogleServiceHelperMethods;
import org.springframework.stereotype.Service;

import java.util.Collections;
import java.util.List;

@Service
public class GoogleSharedDriveService {
    private final GoogleSharedDriveCacheService sharedDriveCacheService;

    public GoogleSharedDriveService(GoogleSharedDriveCacheService sharedDriveCacheService) {
        this.sharedDriveCacheService = sharedDriveCacheService;
    }

    public void forceRefreshCache(String loggedInEmail) {
        sharedDriveCacheService.forceRefreshCache(loggedInEmail);
    }

    public SharedDrivePageResponse getSharedDrivesPaged(String loggedInEmail, String pageToken, int size, String query) {
        SharedDriveCacheEntry cachedData = sharedDriveCacheService.getOrFetchDriveData(loggedInEmail);

        List<SharedDriveBasicDetail> filteredList = cachedData.allDrives();
        if (query != null && !query.trim().isEmpty()) {
            String lowerQuery = query.toLowerCase().trim();
            filteredList = filteredList.stream()
                    .filter(d -> d.name() != null && d.name().toLowerCase().contains(lowerQuery))
                    .toList();
        }

        int page = GoogleServiceHelperMethods.getPage(pageToken);

        int totalDrives = filteredList.size();
        int startIndex = (page - 1) * size;
        int endIndex = Math.min(startIndex + size, totalDrives);

        List<SharedDriveBasicDetail> pagedItems = (startIndex >= totalDrives)
                ? Collections.emptyList()
                : filteredList.subList(startIndex, endIndex);

        String nextTokenToReturn = (endIndex < totalDrives) ? String.valueOf(page + 1) : null;
        return new SharedDrivePageResponse(pagedItems, nextTokenToReturn);
    }

    public SharedDriveOverviewResponse getDrivesPageOverview(String loggedInEmail) {
            SharedDriveCacheEntry cachedData = sharedDriveCacheService.getOrFetchDriveData(loggedInEmail);
            List<SharedDriveBasicDetail> drives = cachedData.allDrives();

            int totalDrives = drives.size();

        int orphanDrives = (int) drives.stream().filter(d -> d.totalOrganizers() <= 0).count();
        int totalLowRisk = (int) drives.stream().filter(d -> d.risk().equals("Laag")).count();
        int totalMediumRisk = (int) drives.stream().filter(d -> d.risk().equals("Middel")).count();
        int totalHighRisk = (int) drives.stream().filter(d -> d.risk().equals("Hoog")).count();
        int totalExternalMembersCount = (int) drives.stream().filter(d -> d.externalMembers() > 0).count();

        int securityScore = totalDrives == 0 ? 0 : (int) Math.round((totalLowRisk * 100.0 + totalMediumRisk * 60.0 + totalHighRisk * 20.0) / totalDrives);

        int notOnlyDomainUsersAllowedCount = (int) drives.stream().filter(d -> !d.onlyDomainUsersAllowed()).count();
        int notOnlyMembersCanAccessCount = (int) drives.stream().filter(d -> !d.onlyMembersCanAccess()).count();
        int externalMembersDriveCount = (int) drives.stream().filter(d -> d.externalMembers() > 0).count();

            return new SharedDriveOverviewResponse(
                    totalDrives,
                    orphanDrives,
                    totalHighRisk,
                    totalExternalMembersCount,
                    securityScore,
                    notOnlyDomainUsersAllowedCount,
                    notOnlyMembersCanAccessCount,
                    externalMembersDriveCount
            );

    }


}
