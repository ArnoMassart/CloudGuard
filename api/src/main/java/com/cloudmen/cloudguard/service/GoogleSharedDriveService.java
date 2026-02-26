package com.cloudmen.cloudguard.service;

import com.cloudmen.cloudguard.dto.drives.*;
import com.cloudmen.cloudguard.service.cache.GoogleSharedDriveCacheService;
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

        // 1. Filter IN HET GEHEUGEN
        List<SharedDriveBasicDetail> filteredList = cachedData.allDrives();
        if (query != null && !query.trim().isEmpty()) {
            String lowerQuery = query.toLowerCase().trim();
            filteredList = filteredList.stream()
                    .filter(d -> d.name() != null && d.name().toLowerCase().contains(lowerQuery))
                    .toList();
        }

        // 2. Pagineren IN HET GEHEUGEN
        int page = 1;
        if (pageToken != null && !pageToken.isBlank()) {
            try { page = Integer.parseInt(pageToken); } catch (NumberFormatException ignored) {}
        }

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

            long totalDrives = drives.size();

            long orphanDrives = drives.stream().filter(d -> d.totalOrganizers() <= 0).count();
            long totalLowRisk = drives.stream().filter(d -> d.risk().equals("Laag")).count();
            long totalMediumRisk = drives.stream().filter(d -> d.risk().equals("Middel")).count();
            long totalHighRisk = drives.stream().filter(d -> d.risk().equals("Hoog")).count();
            long totalExternalMembersCount = drives.stream().filter(d -> d.externalMembers() > 0).count();

            long securityScore = totalDrives == 0 ? 0 : (int) Math.round((totalLowRisk * 100.0 + totalMediumRisk * 60.0 + totalHighRisk * 20.0) / totalDrives);

            long notOnlyDomainUsersAllowedCount = drives.stream().filter(d -> !d.onlyDomainUsersAllowed()).count();
            long notOnlyMembersCanAccessCount = drives.stream().filter(d -> !d.onlyMembersCanAccess()).count();
            long externalMembersDriveCount = drives.stream().filter(d -> d.externalMembers() > 0).count();

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
