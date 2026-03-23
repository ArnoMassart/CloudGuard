package com.cloudmen.cloudguard.service;

import com.cloudmen.cloudguard.dto.groups.*;
import com.cloudmen.cloudguard.dto.password.SecurityScoreBreakdownDto;
import com.cloudmen.cloudguard.dto.password.SecurityScoreFactorDto;
import com.cloudmen.cloudguard.service.cache.GoogleGroupsCacheService;
import com.cloudmen.cloudguard.utility.GoogleServiceHelperMethods;
import org.springframework.stereotype.Service;

import java.util.Collections;
import java.util.List;

@Service
public class GoogleGroupsService {
    private final GoogleGroupsCacheService groupsCacheService;


    public GoogleGroupsService(GoogleGroupsCacheService groupsCacheService) {
        this.groupsCacheService = groupsCacheService;
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

        long totalGroups = cachedData.allGroups().size();
        long groupsWithExternal = 0;
        long highRiskGroups = 0;
        long mediumRiskGroups = 0;
        long lowRiskGroups = 0;

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
                totalGroups, groupsWithExternal, highRiskGroups, mediumRiskGroups, lowRiskGroups, securityScore);

        return new GroupOverviewResponse(
                totalGroups, groupsWithExternal, highRiskGroups, mediumRiskGroups, lowRiskGroups, securityScore, breakdown
        );
    }

    private SecurityScoreBreakdownDto buildGroupsBreakdown(long totalGroups, long groupsWithExternal, long highRiskGroups, long mediumRiskGroups, long lowRiskGroups, int securityScore) {
        int lowScore = totalGroups == 0 ? 100 : (int) Math.round(lowRiskGroups * 100.0 / totalGroups);
        int mediumScore = totalGroups == 0 ? 0 : (int) Math.round(mediumRiskGroups * 60.0 / totalGroups);
        int highScore = totalGroups == 0 ? 0 : (int) Math.round(highRiskGroups * 20.0 / totalGroups);
        int externalScore = totalGroups == 0 ? 100 : groupsWithExternal == 0 ? 100 : (int) Math.max(0, 100 - groupsWithExternal * 100 / totalGroups);

        var factors = java.util.List.of(
                new SecurityScoreFactorDto("Laag risico groepen", lowRiskGroups + " van " + totalGroups + " groepen met laag risico", lowScore, 100, severity(lowScore)),
                new SecurityScoreFactorDto("Gemiddeld risico groepen", mediumRiskGroups + " van " + totalGroups + " groepen met gemiddeld risico", mediumScore, 60, severity(mediumScore * 100 / 60)),
                new SecurityScoreFactorDto("Hoog risico groepen", highRiskGroups + " van " + totalGroups + " groepen met hoog risico (open/externe instellingen)", highScore, 20, severity(highScore * 100 / 20)),
                new SecurityScoreFactorDto("Groepen zonder externe leden", groupsWithExternal == 0 ? "Geen groepen met externe leden" : groupsWithExternal + " groep(en) met externe leden", externalScore, 100, severity(externalScore))
        );
        String status = securityScore == 100 ? "perfect" : securityScore >= 75 ? "good" : securityScore > 50 ? "average" : "bad";
        return new SecurityScoreBreakdownDto(securityScore, status, factors);
    }

    private static String severity(double score) {
        if (score >= 75) return "success";
        if (score >= 50) return "warning";
        return "error";
    }
}
