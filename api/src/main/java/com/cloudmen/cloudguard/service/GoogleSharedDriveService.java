package com.cloudmen.cloudguard.service;

import com.cloudmen.cloudguard.dto.drives.SharedDriveBasicDetail;
import com.cloudmen.cloudguard.dto.drives.SharedDriveCacheEntry;
import com.cloudmen.cloudguard.dto.drives.SharedDriveOverviewResponse;
import com.cloudmen.cloudguard.dto.drives.SharedDrivePageResponse;
import com.cloudmen.cloudguard.dto.password.SecurityScoreBreakdownDto;
import com.cloudmen.cloudguard.dto.password.SecurityScoreFactorDto;
import com.cloudmen.cloudguard.dto.preferences.SectionWarningsDto;
import com.cloudmen.cloudguard.service.cache.GoogleSharedDriveCacheService;
import com.cloudmen.cloudguard.service.preference.SecurityPreferenceScoreSupport;
import com.cloudmen.cloudguard.service.preference.SectionWarningEvaluator;
import com.cloudmen.cloudguard.utility.DateTimeConverter;
import com.cloudmen.cloudguard.utility.GoogleServiceHelperMethods;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.MessageSource;
import org.springframework.context.i18n.LocaleContextHolder;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Set;
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
        return getDrivesPageOverview(loggedInEmail, Set.of());
    }

    public SharedDriveOverviewResponse getDrivesPageOverview(String loggedInEmail, Set<String> disabledKeys) {
        Set<String> off = disabledKeys == null ? Set.of() : disabledKeys;
        SharedDriveCacheEntry cachedData = sharedDriveCacheService.getOrFetchDriveData(loggedInEmail);
        List<SharedDriveBasicDetail> drives = cachedData.allDrives();

        int totalDrives = drives.size();

        int orphanDrives = (int) drives.stream().filter(d -> d.totalOrganizers() <= 0).count();
        int totalLowRisk = (int) drives.stream().filter(d -> d.risk().equals("low")).count();
        int totalMediumRisk = (int) drives.stream().filter(d -> d.risk().equals("middle")).count();
        int totalHighRisk = (int) drives.stream().filter(d -> d.risk().equals("high")).count();
        int totalExternalMembersCount = (int) drives.stream().filter(d -> d.externalMembers() > 0).count();

        int riskOnlyScore = totalDrives == 0 ? 100
                : (int) Math.round((totalLowRisk * 100.0 + totalMediumRisk * 60.0 + totalHighRisk * 20.0) / totalDrives);

        int notOnlyDomainUsersAllowedCount = (int) drives.stream().filter(d -> !d.onlyDomainUsersAllowed()).count();
        int notOnlyMembersCanAccessCount = (int) drives.stream().filter(d -> !d.onlyMembersCanAccess()).count();
        int externalMembersDriveCount = (int) drives.stream().filter(d -> d.externalMembers() > 0).count();

        SecurityScoreBreakdownDto breakdown = buildDrivesBreakdown(
                totalDrives, totalLowRisk, totalMediumRisk, totalHighRisk, orphanDrives,
                notOnlyDomainUsersAllowedCount, notOnlyMembersCanAccessCount,
                riskOnlyScore, off);

        SectionWarningsDto warnings = SectionWarningEvaluator.with(off)
                .check("notOnlyDomainUsersAllowedWarning", notOnlyDomainUsersAllowedCount, "shared-drives", "outsideDomain")
                .check("notOnlyMembersCanAccessWarning", notOnlyMembersCanAccessCount, "shared-drives", "nonMemberAccess")
                .check("externalMembersWarning", externalMembersDriveCount, "shared-drives", "external")
                .check("orphanDrivesWarning", orphanDrives, "shared-drives", "orphan")
                .build();

        return new SharedDriveOverviewResponse(
                totalDrives,
                orphanDrives,
                totalHighRisk,
                totalExternalMembersCount,
                breakdown.totalScore(),
                notOnlyDomainUsersAllowedCount,
                notOnlyMembersCanAccessCount,
                externalMembersDriveCount,
                breakdown,
                warnings
        );
    }

    private SecurityScoreBreakdownDto buildDrivesBreakdown(int totalDrives, int totalLowRisk, int totalMediumRisk, int totalHighRisk,
                                                            int orphanDrives, int notOnlyDomainUsersAllowedCount,
                                                            int notOnlyMembersCanAccessCount,
                                                            int riskOnlyScore, Set<String> off) {
        int lowScore = totalDrives == 0 ? 100 : (int) Math.round(totalLowRisk * 100.0 / totalDrives);
        int mediumScore = totalDrives == 0 ? 0 : (int) Math.round(totalMediumRisk * 60.0 / totalDrives);
        int highScore = totalDrives == 0 ? 0 : (int) Math.round(totalHighRisk * 20.0 / totalDrives);
        int orphanScore = totalDrives == 0 ? 100 : orphanDrives == 0 ? 100 : (int) Math.max(0, 100 - orphanDrives * 100 / totalDrives);
        int domainOnlyScore = totalDrives == 0 ? 100 : notOnlyDomainUsersAllowedCount == 0 ? 100 : (int) Math.max(0, 100 - notOnlyDomainUsersAllowedCount * 50 / totalDrives);
        int membersOnlyScore = totalDrives == 0 ? 100 : notOnlyMembersCanAccessCount == 0 ? 100 : (int) Math.max(0, 100 - notOnlyMembersCanAccessCount * 50 / totalDrives);

        if (SecurityPreferenceScoreSupport.preferenceDisabled(off, "shared-drives", "orphan")) {
            orphanScore = 100;
        }
        if (SecurityPreferenceScoreSupport.preferenceDisabled(off, "shared-drives", "outsideDomain")) {
            domainOnlyScore = 100;
        }
        if (SecurityPreferenceScoreSupport.preferenceDisabled(off, "shared-drives", "nonMemberAccess")) {
            membersOnlyScore = 100;
        }

        int combinedScore = combinedDriveSecurityScore(totalDrives, riskOnlyScore, orphanScore, domainOnlyScore, membersOnlyScore, off);

        boolean muteRisk = SecurityPreferenceScoreSupport.preferenceDisabled(off, "shared-drives", "external");
        boolean muteOrphan = SecurityPreferenceScoreSupport.preferenceDisabled(off, "shared-drives", "orphan");
        boolean muteDomain = SecurityPreferenceScoreSupport.preferenceDisabled(off, "shared-drives", "outsideDomain");
        boolean muteMembers = SecurityPreferenceScoreSupport.preferenceDisabled(off, "shared-drives", "nonMemberAccess");

        Locale locale = LocaleContextHolder.getLocale();

        var factors = java.util.List.of(
                new SecurityScoreFactorDto(messageSource.getMessage("drives.score.factor.low_risk.title", null, locale), messageSource.getMessage("drives.score.factor.low_risk.description", new Object[]{totalLowRisk, totalDrives}, locale), lowScore, 100, severity(lowScore), muteRisk),
                new SecurityScoreFactorDto(messageSource.getMessage("drives.score.factor.middle_risk.title", null, locale), messageSource.getMessage("drives.score.factor.middle_risk.description", new Object[]{totalMediumRisk, totalDrives}, locale), mediumScore, 60, severity(mediumScore > 0 ? mediumScore * 100 / 60 : 0), muteRisk),
                new SecurityScoreFactorDto(messageSource.getMessage("drives.score.factor.high_risk.title", null, locale), messageSource.getMessage("drives.score.factor.high_risk.description", new Object[]{totalHighRisk, totalDrives}, locale), highScore, 20, severity(highScore > 0 ? highScore * 100 / 20 : 0), muteRisk),
                new SecurityScoreFactorDto(messageSource.getMessage("drives.score.factor.with_managers.title", null, locale), orphanDrives == 0 ? messageSource.getMessage("drives.score.factor.with_managers_not.description", null, locale) : messageSource.getMessage("drives.score.factor.with_managers.description", new Object[]{orphanDrives}, locale), orphanScore, 100, severity(orphanScore), muteOrphan),
                new SecurityScoreFactorDto(messageSource.getMessage("drives.score.factor.only_domain.title", null, locale), notOnlyDomainUsersAllowedCount == 0 ? messageSource.getMessage("drives.score.factor.only_domain_not.description", null, locale) : messageSource.getMessage("drives.score.factor.only_domain.description", new Object[]{notOnlyDomainUsersAllowedCount}, locale), domainOnlyScore, 100, severity(domainOnlyScore), muteDomain),
                new SecurityScoreFactorDto(messageSource.getMessage("drives.score.factor.only_members.title", null, locale), notOnlyMembersCanAccessCount == 0 ? messageSource.getMessage("drives.score.factor.only_members_not.description", null, locale) : messageSource.getMessage("drives.score.factor.only_members.description", new Object[]{notOnlyMembersCanAccessCount}, locale), membersOnlyScore, 100, severity(membersOnlyScore), muteMembers)
        );
        String status = combinedScore == 100 ? "perfect" : combinedScore >= 75 ? "good" : combinedScore > 50 ? "average" : "bad";
        return new SecurityScoreBreakdownDto(combinedScore, status, factors);
    }

    private static int combinedDriveSecurityScore(int totalDrives, int riskOnlyScore, int orphanScore,
                                                  int domainOnlyScore, int membersOnlyScore, Set<String> off) {
        if (totalDrives == 0) {
            return 100;
        }
        double sum = 0;
        int parts = 0;
        if (!SecurityPreferenceScoreSupport.preferenceDisabled(off, "shared-drives", "external")) {
            sum += riskOnlyScore;
            parts++;
        }
        if (!SecurityPreferenceScoreSupport.preferenceDisabled(off, "shared-drives", "orphan")) {
            sum += orphanScore;
            parts++;
        }
        if (!SecurityPreferenceScoreSupport.preferenceDisabled(off, "shared-drives", "outsideDomain")) {
            sum += domainOnlyScore;
            parts++;
        }
        if (!SecurityPreferenceScoreSupport.preferenceDisabled(off, "shared-drives", "nonMemberAccess")) {
            sum += membersOnlyScore;
            parts++;
        }
        if (parts == 0) {
            return 100;
        }
        return (int) Math.round(sum / parts);
    }

    private static String severity(double score) {
        if (score >= 75) return "success";
        if (score >= 50) return "warning";
        return "error";
    }

}
