package com.cloudmen.cloudguard.service;

import com.cloudmen.cloudguard.dto.drives.SharedDriveBasicDetail;
import com.cloudmen.cloudguard.dto.drives.SharedDriveCacheEntry;
import com.cloudmen.cloudguard.dto.drives.SharedDriveOverviewResponse;
import com.cloudmen.cloudguard.dto.drives.SharedDrivePageResponse;
import com.cloudmen.cloudguard.dto.password.SecurityScoreBreakdownDto;
import com.cloudmen.cloudguard.dto.password.SecurityScoreFactorDto;
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

        int securityScore = totalDrives == 0 ? 100 : (int) Math.round((totalLowRisk * 100.0 + totalMediumRisk * 60.0 + totalHighRisk * 20.0) / totalDrives);

        int notOnlyDomainUsersAllowedCount = (int) drives.stream().filter(d -> !d.onlyDomainUsersAllowed()).count();
        int notOnlyMembersCanAccessCount = (int) drives.stream().filter(d -> !d.onlyMembersCanAccess()).count();
        int externalMembersDriveCount = (int) drives.stream().filter(d -> d.externalMembers() > 0).count();

        SecurityScoreBreakdownDto breakdown = buildDrivesBreakdown(
                totalDrives, totalLowRisk, totalMediumRisk, totalHighRisk, orphanDrives,
                notOnlyDomainUsersAllowedCount, notOnlyMembersCanAccessCount, externalMembersDriveCount, securityScore);

            return new SharedDriveOverviewResponse(
                    totalDrives,
                    orphanDrives,
                    totalHighRisk,
                    totalExternalMembersCount,
                    securityScore,
                    notOnlyDomainUsersAllowedCount,
                    notOnlyMembersCanAccessCount,
                    externalMembersDriveCount,
                    breakdown
            );

    }

    private SecurityScoreBreakdownDto buildDrivesBreakdown(int totalDrives, int totalLowRisk, int totalMediumRisk, int totalHighRisk, int orphanDrives,
                                                          int notOnlyDomainUsersAllowedCount, int notOnlyMembersCanAccessCount, int externalMembersDriveCount, int securityScore) {
        int lowScore = totalDrives == 0 ? 100 : (int) Math.round(totalLowRisk * 100.0 / totalDrives);
        int mediumScore = totalDrives == 0 ? 0 : (int) Math.round(totalMediumRisk * 60.0 / totalDrives);
        int highScore = totalDrives == 0 ? 0 : (int) Math.round(totalHighRisk * 20.0 / totalDrives);
        int orphanScore = totalDrives == 0 ? 100 : orphanDrives == 0 ? 100 : (int) Math.max(0, 100 - orphanDrives * 100 / totalDrives);
        int domainOnlyScore = totalDrives == 0 ? 100 : notOnlyDomainUsersAllowedCount == 0 ? 100 : (int) Math.max(0, 100 - notOnlyDomainUsersAllowedCount * 50 / totalDrives);
        int membersOnlyScore = totalDrives == 0 ? 100 : notOnlyMembersCanAccessCount == 0 ? 100 : (int) Math.max(0, 100 - notOnlyMembersCanAccessCount * 50 / totalDrives);

        var factors = java.util.List.of(
                new SecurityScoreFactorDto("Laag risico drives", totalLowRisk + " van " + totalDrives + " drives met laag risico", 40, lowScore, 100, severity(lowScore)),
                new SecurityScoreFactorDto("Gemiddeld risico drives", totalMediumRisk + " van " + totalDrives + " drives met gemiddeld risico", 20, mediumScore, 60, severity(mediumScore > 0 ? mediumScore * 100 / 60 : 0)),
                new SecurityScoreFactorDto("Hoog risico drives", totalHighRisk + " van " + totalDrives + " drives met hoog risico", 15, highScore, 20, severity(highScore > 0 ? highScore * 100 / 20 : 0)),
                new SecurityScoreFactorDto("Drives met beheerders", orphanDrives == 0 ? "Alle drives hebben beheerders" : orphanDrives + " drive(s) zonder beheerder", 10, orphanScore, 100, severity(orphanScore)),
                new SecurityScoreFactorDto("Alleen domeingebruikers", notOnlyDomainUsersAllowedCount == 0 ? "Alle drives staan alleen domeingebruikers toe" : notOnlyDomainUsersAllowedCount + " drive(s) staan externe gebruikers toe", 8, domainOnlyScore, 100, severity(domainOnlyScore)),
                new SecurityScoreFactorDto("Alleen leden toegang", notOnlyMembersCanAccessCount == 0 ? "Alle drives beperken toegang tot leden" : notOnlyMembersCanAccessCount + " drive(s) geven toegang aan niet-leden", 7, membersOnlyScore, 100, severity(membersOnlyScore))
        );
        String status = securityScore == 100 ? "Perfect" : securityScore >= 75 ? "Goed" : securityScore > 50 ? "Matig" : "Slecht";
        return new SecurityScoreBreakdownDto(securityScore, status, factors);
    }

    private static String severity(double score) {
        if (score >= 75) return "success";
        if (score >= 50) return "warning";
        return "error";
    }

}
