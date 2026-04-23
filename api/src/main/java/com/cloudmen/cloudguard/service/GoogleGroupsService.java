package com.cloudmen.cloudguard.service;

import com.cloudmen.cloudguard.dto.groups.*;
import com.cloudmen.cloudguard.dto.password.SecurityScoreBreakdownDto;
import com.cloudmen.cloudguard.dto.password.SecurityScoreFactorDto;
import com.cloudmen.cloudguard.dto.preferences.SectionWarningsDto;
import com.cloudmen.cloudguard.service.cache.GoogleGroupsCacheService;
import com.cloudmen.cloudguard.service.preference.SecurityPreferenceScoreSupport;
import com.cloudmen.cloudguard.service.preference.SectionWarningEvaluator;
import com.cloudmen.cloudguard.utility.GoogleServiceHelperMethods;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.MessageSource;
import org.springframework.context.i18n.LocaleContextHolder;
import org.springframework.stereotype.Service;

import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.Locale;

import static com.cloudmen.cloudguard.utility.GoogleServiceHelperMethods.securityScoreFactorForDetail;
import static com.cloudmen.cloudguard.utility.GoogleServiceHelperMethods.severity;

@Service
public class GoogleGroupsService {
    private final GoogleGroupsCacheService groupsCacheService;
    private final MessageSource messageSource;


    public GoogleGroupsService(GoogleGroupsCacheService groupsCacheService, @Qualifier("messageSource") MessageSource messageSource) {
        this.groupsCacheService = groupsCacheService;
        this.messageSource = messageSource;
    }

    public void forceRefreshCache(String loggedInEmail) {
        groupsCacheService.forceRefreshCache(loggedInEmail);
    }

    public GroupPageResponse getGroupsPaged(String loggedInEmail, String query, String pageToken, int size) {
        GroupCacheEntry cachedData = groupsCacheService.getOrFetchGroupData(loggedInEmail);

        List<CachedGroupItem> filteredList = GoogleServiceHelperMethods.filterByNameOrEmail(cachedData.allGroups(), query, CachedGroupItem::name, CachedGroupItem::email);

        int page = GoogleServiceHelperMethods.getPage(pageToken);

        int totalGroups = filteredList.size();
        int startIndex = (page - 1) * size;
        int endIndex = Math.min(startIndex + size, totalGroups);

        List<CachedGroupItem> pagedItems = (startIndex >= totalGroups) ? Collections.emptyList() : filteredList.subList(startIndex, endIndex);

        List<GroupOrgDetail> result = pagedItems.stream().map(CachedGroupItem::detail).toList();

        String nextTokenToReturn = (endIndex < totalGroups) ? String.valueOf(page + 1) : null;
        return new GroupPageResponse(result, nextTokenToReturn);
    }

    public GroupOverviewResponse getGroupsOverview(String loggedInEmail) {
        GroupCacheEntry cachedData = groupsCacheService.getOrFetchGroupData(loggedInEmail);

        int totalGroups = cachedData.allGroups().size();
        int groupsWithExternal = 0;
        int highRiskGroups = 0;
        int mediumRiskGroups = 0;
        int lowRiskGroups = 0;

        for (CachedGroupItem item : cachedData.allGroups()) {
            GroupOrgDetail detail = item.detail();

            if (detail.getExternalMembers() > 0 || detail.isExternalAllowed()) {
                groupsWithExternal++;
            }

            switch (detail.getRisk()) {
                case "HIGH" -> highRiskGroups++;
                case "MEDIUM" -> mediumRiskGroups++;
                default -> lowRiskGroups++;
            }
        }

        int securityScore = totalGroups == 0 ? 100
                : (int) Math.round((lowRiskGroups * 100.0 + mediumRiskGroups * 60.0 + highRiskGroups * 20.0) / totalGroups);

        SecurityScoreBreakdownDto breakdown = buildGroupsBreakdown(
                totalGroups, groupsWithExternal, highRiskGroups, mediumRiskGroups, lowRiskGroups, securityScore, false);

        return new GroupOverviewResponse(
                totalGroups, groupsWithExternal, highRiskGroups, mediumRiskGroups, lowRiskGroups, securityScore, breakdown, null
        );
    }

    public GroupOverviewResponse getGroupsOverview(String loggedInEmail, Set<String> disabledKeys) {
        Set<String> off = disabledKeys == null ? Set.of() : disabledKeys;
        GroupOverviewResponse base = getGroupsOverview(loggedInEmail);
        boolean ignoreGroupRisk = SecurityPreferenceScoreSupport.preferenceDisabled(off, "users-groups", "groupExternal");
        int securityScore = ignoreGroupRisk ? 100 : base.securityScore();

        SecurityScoreBreakdownDto breakdown = ignoreGroupRisk
                ? buildGroupsBreakdown(base.totalGroups(), base.groupsWithExternal(), base.highRiskGroups(),
                base.mediumRiskGroups(), base.lowRiskGroups(), 100, true)
                : base.securityScoreBreakdown();
        SectionWarningsDto warnings = SectionWarningEvaluator.with(off)
                .check("externalMember", base.groupsWithExternal(), "users-groups", "groupExternal")
                .check("highRisk", base.highRiskGroups(), "users-groups", "groupExternal")
                .build();
        return new GroupOverviewResponse(
                base.totalGroups(), base.groupsWithExternal(), base.highRiskGroups(),
                base.mediumRiskGroups(), base.lowRiskGroups(), securityScore,
                breakdown, warnings);
    }

    private SecurityScoreBreakdownDto buildGroupsBreakdown(int totalGroups, int groupsWithExternal, int highRiskGroups, int mediumRiskGroups, int lowRiskGroups, int securityScore,
                                                           boolean neutralizeForDisabledPref) {
        int lowScore = GoogleServiceHelperMethods.calculateWeightedScore(totalGroups, lowRiskGroups, 100.0, 100);
        int mediumScore = GoogleServiceHelperMethods.calculateWeightedScore(totalGroups, mediumRiskGroups, 60.0, 100);
        int highScore = GoogleServiceHelperMethods.calculateWeightedScore(totalGroups, highRiskGroups, 20.0, 100);
        int externalScore = GoogleServiceHelperMethods.calculateDeductionScore(totalGroups, groupsWithExternal);
        if (neutralizeForDisabledPref) {
            lowScore = 100;
            mediumScore = 60;
            highScore = 20;
            externalScore = 100;
        }

        Locale locale = LocaleContextHolder.getLocale();

        /*
         * Only list a risk tier when there is at least one group in that tier. Otherwise the weighted
         * slice shows 0/N with error styling even though "zero groups in this tier" is not a failure
         * (e.g. no high-risk groups is good).
         */
        boolean showLowTier = neutralizeForDisabledPref || (totalGroups > 0 && lowRiskGroups > 0);
        boolean showMediumTier = neutralizeForDisabledPref || (totalGroups > 0 && mediumRiskGroups > 0);
        boolean showHighTier = neutralizeForDisabledPref || (totalGroups > 0 && highRiskGroups > 0);

        var factors = java.util.List.of(
                securityScoreFactorForDetail(
                        showLowTier,
                        messageSource.getMessage("groups.score.factor.low_risk.title", null, locale),
                        messageSource.getMessage("groups.score.factor.low_risk.description", new Object[]{lowRiskGroups, totalGroups}, locale),
                        lowScore,
                        100,
                        severity(lowScore),
                        neutralizeForDisabledPref),
                securityScoreFactorForDetail(
                        showMediumTier,
                        messageSource.getMessage("groups.score.factor.middle_risk.title", null, locale),
                        messageSource.getMessage("groups.score.factor.middle_risk.description", new Object[]{mediumRiskGroups, totalGroups}, locale),
                        mediumScore,
                        60,
                        severity(mediumScore * 100.0 / 60),
                        neutralizeForDisabledPref),
                securityScoreFactorForDetail(
                        showHighTier,
                        messageSource.getMessage("groups.score.factor.high_risk.title", null, locale),
                        messageSource.getMessage("groups.score.factor.high_risk.description", new Object[]{highRiskGroups, totalGroups}, locale),
                        highScore,
                        20,
                        severity(highScore * 100.0 / 20),
                        neutralizeForDisabledPref),
                new SecurityScoreFactorDto(messageSource.getMessage("groups.score.factor.no_external.title", null, locale), groupsWithExternal == 0 ? messageSource.getMessage("groups.score.factor.no_external.description.no_found", null, locale) : messageSource.getMessage("groups.score.factor.no_external.description", new Object[]{groupsWithExternal}, locale), externalScore, 100, severity(externalScore), neutralizeForDisabledPref)
        );
        String status = securityScore == 100 ? "perfect" : securityScore >= 75 ? "good" : securityScore > 50 ? "average" : "bad";
        return new SecurityScoreBreakdownDto(securityScore, status, factors);
    }
}
