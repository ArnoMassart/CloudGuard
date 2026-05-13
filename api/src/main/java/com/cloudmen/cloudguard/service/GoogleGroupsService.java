package com.cloudmen.cloudguard.service;

import com.cloudmen.cloudguard.dto.groups.*;
import com.cloudmen.cloudguard.dto.password.SecurityScoreBreakdownDto;
import com.cloudmen.cloudguard.dto.password.SecurityScoreFactorDto;
import com.cloudmen.cloudguard.dto.preferences.SectionWarningsDto;
import com.cloudmen.cloudguard.service.cache.GoogleGroupsCacheService;
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

import static com.cloudmen.cloudguard.utility.GoogleServiceHelperMethods.getOverviewStatus;
import static com.cloudmen.cloudguard.utility.GoogleServiceHelperMethods.securityScoreFactorForDetail;
import static com.cloudmen.cloudguard.utility.GoogleServiceHelperMethods.severity;

/**
 * Application service for the <strong>Users &amp; Groups</strong> module’s <em>groups</em> slice: reads cached
 * {@link GroupCacheEntry} data from {@link GoogleGroupsCacheService}, applies search and paging, and builds overview
 * metrics plus a weighted security score and factor breakdown. The overload taking disabled preference keys attaches
 * {@link com.cloudmen.cloudguard.dto.preferences.SectionWarningsDto} so the UI can reflect org policy without changing the headline score.
 */
@Service
public class GoogleGroupsService {
    private final GoogleGroupsCacheService groupsCacheService;
    private final MessageSource messageSource;

    /**
     * @param groupsCacheService tenant-scoped cache of Directory + settings snapshot
     * @param messageSource      bundle for {@code groups.score.factor.*} breakdown strings
     */
    public GoogleGroupsService(GoogleGroupsCacheService groupsCacheService, @Qualifier("messageSource") MessageSource messageSource) {
        this.groupsCacheService = groupsCacheService;
        this.messageSource = messageSource;
    }

    /** Triggers a synchronous refresh of the tenant’s Google Groups cache entry. */
    public void forceRefreshCache(String loggedInEmail) {
        groupsCacheService.forceRefreshCache(loggedInEmail);
    }

    /**
     * Returns one page of {@link GroupOrgDetail} after optional substring filter on {@link CachedGroupItem}
     * display name or email, using a 1-based string {@code pageToken} cursor.
     *
     * @param loggedInEmail authenticated user (workspace admin is resolved inside the cache layer)
     * @param query         optional filter; blank or null lists all groups in cache order
     * @param pageToken     previous response’s next token, or {@code null} for the first page
     * @param size          maximum rows per page
     */
    public GroupPageResponse getGroupsPaged(String loggedInEmail, String query, String pageToken, int size) {
        GroupCacheEntry cachedData = groupsCacheService.getOrFetchData(loggedInEmail);

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

    /**
     * Raw overview from cache without applying org security preferences (used as the base for
     * {@link #getGroupsOverview(String, Set)}).
     * <p>
     * Counting rules: {@code groupsWithExternal} increments per group with at least one external member
     * ({@link GroupOrgDetail#getExternalMembers()} {@code > 0}); {@code groupsWithExternalAllowed} increments when
     * {@link GroupOrgDetail#isExternalAllowed()} is true. Risk buckets follow {@link GroupOrgDetail#getRisk()}.
     * The headline {@code securityScore} is a simple tier-weighted average (LOW=100, MEDIUM=60, HIGH=20 points per group)
     * and is {@code null} when there are no groups.
     *
     * @param loggedInEmail authenticated user email used to resolve cached workspace groups
     */
    public GroupOverviewResponse getGroupsOverview(String loggedInEmail) {
        GroupCacheEntry cachedData = groupsCacheService.getOrFetchData(loggedInEmail);

        int totalGroups = cachedData.allGroups().size();
        int groupsWithExternal = 0;
        int groupsWithExternalAllowed = 0;
        int highRiskGroups = 0;
        int mediumRiskGroups = 0;
        int lowRiskGroups = 0;

        for (CachedGroupItem item : cachedData.allGroups()) {
            GroupOrgDetail detail = item.detail();

            if (detail.isExternalAllowed()) {
                groupsWithExternalAllowed++;
            }
            if (detail.getExternalMembers() > 0) {
                groupsWithExternal++;
            }

            switch (detail.getRisk()) {
                case "HIGH" -> highRiskGroups++;
                case "MEDIUM" -> mediumRiskGroups++;
                default -> lowRiskGroups++;
            }
        }

        if (totalGroups == 0) {
            return new GroupOverviewResponse(
                    0, 0, 0 ,0, 0, 0,
                    null,
                    null,
                    null
            );
        }

        int securityScore = (int) Math.round((lowRiskGroups * 100.0 + mediumRiskGroups * 60.0 + highRiskGroups * 20.0) / totalGroups);

        SecurityScoreBreakdownDto breakdown = buildGroupsBreakdown(
                totalGroups, groupsWithExternal, groupsWithExternalAllowed ,highRiskGroups, mediumRiskGroups, lowRiskGroups, securityScore);

        return new GroupOverviewResponse(
                totalGroups, groupsWithExternal,groupsWithExternalAllowed, highRiskGroups, mediumRiskGroups, lowRiskGroups, securityScore, breakdown, null
        );
    }

    /**
     * Overview for dashboards and the groups UI: same numeric score and breakdown as {@link #getGroupsOverview(String)},
     * plus {@link com.cloudmen.cloudguard.dto.preferences.SectionWarningsDto} describing whether external-member /
     * high-risk checks apply relative to disabled preferences ({@code users-groups:groupExternal} keys in
     * {@code disabledKeys}).
     *
     * @param loggedInEmail authenticated user email used to resolve cached workspace groups
     * @param disabledKeys  set of {@code section:preferenceKey} strings for the org (disabled = key present in the set)
     */
    public GroupOverviewResponse getGroupsOverview(String loggedInEmail, Set<String> disabledKeys) {
        Set<String> off = disabledKeys == null ? Set.of() : disabledKeys;
        GroupOverviewResponse base = getGroupsOverview(loggedInEmail);

        SectionWarningsDto warnings = SectionWarningEvaluator.with(off)
                .check("externalMember", base.groupsWithExternal(), "users-groups", "groupExternal")
                .check("highRisk", base.highRiskGroups(), "users-groups", "groupExternal")
                .build();

        if (base.securityScore() == null) {
            return new GroupOverviewResponse(
                    base.totalGroups(), base.groupsWithExternal(), base.groupsWithExternalAllowed() ,base.highRiskGroups(),
                    base.mediumRiskGroups(), base.lowRiskGroups(), null, null, warnings);
        }

        int securityScore = base.securityScore();

        SecurityScoreBreakdownDto breakdown = base.securityScoreBreakdown();
        return new GroupOverviewResponse(
                base.totalGroups(), base.groupsWithExternal(), base.groupsWithExternalAllowed(), base.highRiskGroups(),
                base.mediumRiskGroups(), base.lowRiskGroups(), securityScore,
                breakdown, warnings);
    }

    /**
     * Builds the translated factor list for the groups score card: low / medium / high risk tiers (weighted subscores),
     * external-member deduction ({@code groupsWithExternal}), and policy openness ({@code groupsWithExternalAllowed}).
     * Rows with no contributing inventory omit tier bars via {@code maxScore == 0} helper behaviour.
     *
     * @param totalGroups                  total cached groups in the snapshot
     * @param groupsWithExternal           groups with external USER members ({@link GroupOrgDetail#getExternalMembers()} {@code > 0})
     * @param groupsWithExternalAllowed    groups whose settings allow external members ({@link GroupOrgDetail#isExternalAllowed()})
     * @param highRiskGroups               count in {@code HIGH} tier
     * @param mediumRiskGroups             count in {@code MEDIUM} tier
     * @param lowRiskGroups                count in {@code LOW} tier (everything not HIGH/MEDIUM)
     * @param securityScore                headline 0–100 score stored on the returned breakdown DTO
     */
    private SecurityScoreBreakdownDto buildGroupsBreakdown(
            int totalGroups,
            int groupsWithExternal,
            int groupsWithExternalAllowed,
            int highRiskGroups,
            int mediumRiskGroups,
            int lowRiskGroups,
            int securityScore) {
        int lowScore = GoogleServiceHelperMethods.calculateWeightedScore(totalGroups, lowRiskGroups, 100.0, 100);
        int mediumScore = GoogleServiceHelperMethods.calculateWeightedScore(totalGroups, mediumRiskGroups, 60.0, 100);
        int highScore = GoogleServiceHelperMethods.calculateWeightedScore(totalGroups, highRiskGroups, 20.0, 100);
        int externalScore = GoogleServiceHelperMethods.calculateDeductionScore(totalGroups, groupsWithExternal);
        int externalAllowedScore = GoogleServiceHelperMethods.calculateDeductionScore(totalGroups, groupsWithExternalAllowed);

        Locale locale = LocaleContextHolder.getLocale();

        boolean showLowTier = (totalGroups > 0 && lowRiskGroups > 0);
        boolean showMediumTier = (totalGroups > 0 && mediumRiskGroups > 0);
        boolean showHighTier = (totalGroups > 0 && highRiskGroups > 0);

        var factors = java.util.List.of(
                securityScoreFactorForDetail(
                        showLowTier,
                        messageSource.getMessage("groups.score.factor.low_risk.title", null, locale),
                        messageSource.getMessage("groups.score.factor.low_risk.description", new Object[]{lowRiskGroups, totalGroups}, locale),
                        lowScore,
                        100,
                        severity(lowScore)),
                securityScoreFactorForDetail(
                        showMediumTier,
                        messageSource.getMessage("groups.score.factor.middle_risk.title", null, locale),
                        messageSource.getMessage("groups.score.factor.middle_risk.description", new Object[]{mediumRiskGroups, totalGroups}, locale),
                        mediumScore,
                        60,
                        severity(mediumScore * 100.0 / 60)),
                securityScoreFactorForDetail(
                        showHighTier,
                        messageSource.getMessage("groups.score.factor.high_risk.title", null, locale),
                        messageSource.getMessage("groups.score.factor.high_risk.description", new Object[]{highRiskGroups, totalGroups}, locale),
                        highScore,
                        20,
                        severity(highScore * 100.0 / 20)),
                new SecurityScoreFactorDto(
                        messageSource.getMessage("groups.score.factor.no_external.title", null, locale),
                        groupsWithExternal == 0 ? messageSource.getMessage("groups.score.factor.no_external.description.no_found", null, locale) : messageSource.getMessage("groups.score.factor.no_external.description", new Object[]{groupsWithExternal}, locale),
                        externalScore,
                        100,
                        severity(externalScore)),
                new SecurityScoreFactorDto(
                        messageSource.getMessage("groups.score.factor.external_members.title", null, locale),
                        groupsWithExternalAllowed == 0 ? messageSource.getMessage("groups.score.factor.external_members.description.none", null, locale) : messageSource.getMessage("groups.score.factor.external_members.description", new Object[]{groupsWithExternalAllowed}, locale),
                        externalAllowedScore,
                        100,
                        severity(externalAllowedScore))

        );
        String status = getOverviewStatus(securityScore);
        return new SecurityScoreBreakdownDto(securityScore, status, factors);
    }
}
