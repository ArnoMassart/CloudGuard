package com.cloudmen.cloudguard.service;

import com.cloudmen.cloudguard.dto.drives.SharedDriveBasicDetail;
import com.cloudmen.cloudguard.dto.drives.SharedDriveCacheEntry;
import com.cloudmen.cloudguard.dto.drives.SharedDriveOverviewResponse;
import com.cloudmen.cloudguard.dto.drives.SharedDrivePageResponse;
import com.cloudmen.cloudguard.dto.password.SecurityScoreBreakdownDto;
import com.cloudmen.cloudguard.dto.password.SecurityScoreFactorDto;
import com.cloudmen.cloudguard.service.cache.GoogleSharedDriveCacheService;
import com.cloudmen.cloudguard.utility.DateTimeConverter;
import com.cloudmen.cloudguard.utility.GoogleServiceHelperMethods;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.MessageSource;
import org.springframework.context.i18n.LocaleContextHolder;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Locale;

import static com.cloudmen.cloudguard.utility.GoogleServiceHelperMethods.severity;

@Service
public class GoogleSharedDriveService {
    private final GoogleSharedDriveCacheService sharedDriveCacheService;
    private final MessageSource messageSource;

    public GoogleSharedDriveService(GoogleSharedDriveCacheService sharedDriveCacheService, @Qualifier("messageSource") MessageSource messageSource) {
        this.sharedDriveCacheService = sharedDriveCacheService;
        this.messageSource = messageSource;
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
                : filteredList.subList(startIndex, endIndex).stream().map(item -> {
            String time = DateTimeConverter.convertToTimeAgo(item.createdTime());
            return new SharedDriveBasicDetail(
                    item.id(),
                    item.name(),
                    item.totalMembers(),
                    item.externalMembers(),
                    item.totalOrganizers(),
                    item.createdTime(),
                    time,
                    item.onlyDomainUsersAllowed(),
                    item.onlyMembersCanAccess(),
                    item.risk()
            );
        }).toList();

        String nextTokenToReturn = (endIndex < totalDrives) ? String.valueOf(page + 1) : null;
        return new SharedDrivePageResponse(pagedItems, nextTokenToReturn);
    }

    public SharedDriveOverviewResponse getDrivesPageOverview(String loggedInEmail) {
            SharedDriveCacheEntry cachedData = sharedDriveCacheService.getOrFetchDriveData(loggedInEmail);
            List<SharedDriveBasicDetail> drives = cachedData.allDrives();

            int totalDrives = drives.size();

        int orphanDrives = (int) drives.stream().filter(d -> d.totalOrganizers() <= 0).count();
        int totalLowRisk = (int) drives.stream().filter(d -> d.risk().equals("low")).count();
        int totalMediumRisk = (int) drives.stream().filter(d -> d.risk().equals("middle")).count();
        int totalHighRisk = (int) drives.stream().filter(d -> d.risk().equals("high")).count();
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

        Locale locale = LocaleContextHolder.getLocale();

        var factors = java.util.List.of(
                new SecurityScoreFactorDto(messageSource.getMessage("drives.score.factor.low_risk.title", null, locale), messageSource.getMessage("drives.score.factor.low_risk.description", new Object[]{totalLowRisk, totalDrives}, locale), lowScore, 100, severity(lowScore)),
                new SecurityScoreFactorDto(messageSource.getMessage("drives.score.factor.middle_risk.title", null, locale), messageSource.getMessage("drives.score.factor.middle_risk.description", new Object[]{totalMediumRisk, totalDrives}, locale), mediumScore, 60, severity(mediumScore > 0 ? mediumScore * 100 / 60 : 0)),
                new SecurityScoreFactorDto(messageSource.getMessage("drives.score.factor.high_risk.title", null, locale), messageSource.getMessage("drives.score.factor.high_risk.description", new Object[]{totalHighRisk, totalDrives}, locale), highScore, 20, severity(highScore > 0 ? highScore * 100 / 20 : 0, true)),
                new SecurityScoreFactorDto(messageSource.getMessage("drives.score.factor.with_managers.title", null, locale), orphanDrives == 0 ? messageSource.getMessage("drives.score.factor.with_managers_not.description", null, locale) : messageSource.getMessage("drives.score.factor.with_managers.description", new Object[]{orphanDrives}, locale), orphanScore, 100, severity(orphanScore)),
                new SecurityScoreFactorDto(messageSource.getMessage("drives.score.factor.only_domain.title", null, locale), notOnlyDomainUsersAllowedCount == 0 ? messageSource.getMessage("drives.score.factor.only_domain_not.description", null, locale) : messageSource.getMessage("drives.score.factor.only_domain.description", new Object[]{notOnlyDomainUsersAllowedCount}, locale), domainOnlyScore, 100, severity(domainOnlyScore)),
                new SecurityScoreFactorDto(messageSource.getMessage("drives.score.factor.only_members.title", null, locale), notOnlyMembersCanAccessCount == 0 ? messageSource.getMessage("drives.score.factor.only_members_not.description", null, locale) : messageSource.getMessage("drives.score.factor.only_members.description", new Object[]{notOnlyMembersCanAccessCount}, locale), membersOnlyScore, 100, severity(membersOnlyScore))
        );
        String status = securityScore == 100 ? "perfect" : securityScore >= 75 ? "good" : securityScore > 50 ? "average" : "bad";
        return new SecurityScoreBreakdownDto(securityScore, status, factors);
    }
}
