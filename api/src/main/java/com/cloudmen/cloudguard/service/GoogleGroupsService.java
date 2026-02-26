package com.cloudmen.cloudguard.service;

import com.cloudmen.cloudguard.dto.groups.*;
import com.cloudmen.cloudguard.service.cache.GoogleGroupsCacheService;
import com.google.api.services.groupssettings.Groupssettings;
import com.google.api.services.groupssettings.model.Groups;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.*;

@Service
public class GoogleGroupsService {

    private static final Logger log = LoggerFactory.getLogger(GoogleGroupsService.class);


    private final GoogleGroupsCacheService groupsCacheService;


    public GoogleGroupsService(GoogleGroupsCacheService groupsCacheService) {
        this.groupsCacheService = groupsCacheService;
    }

    public void forceRefreshCache(String loggedInEmail) {
        groupsCacheService.forceRefreshCache(loggedInEmail);
    }

    public GroupPageResponse getGroupsPaged(String loggedInEmail, String query, String pageToken, int size) {
        // 1. Haal de lijst uit het RAM geheugen (Praat NIET met Google, tenzij de cache leeg is)
        GroupCacheEntry cachedData = groupsCacheService.getOrFetchGroupData(loggedInEmail);

        // 2. Filter IN HET GEHEUGEN
        List<CachedGroupItem> filteredList = cachedData.allGroups();
        if (query != null && !query.trim().isEmpty()) {
            String lowerQuery = query.toLowerCase().trim();
            filteredList = filteredList.stream()
                    .filter(g -> (g.email() != null && g.email().toLowerCase().contains(lowerQuery)) ||
                            (g.name() != null && g.name().toLowerCase().contains(lowerQuery)))
                    .toList();
        }

        // 3. Pagineren IN HET GEHEUGEN
        int page = 1;
        if (pageToken != null && !pageToken.isBlank()) {
            try { page = Integer.parseInt(pageToken); } catch (NumberFormatException ignored) {}
        }

        int totalGroups = filteredList.size();
        int startIndex = (page - 1) * size;
        int endIndex = Math.min(startIndex + size, totalGroups);

        List<CachedGroupItem> pagedItems = (startIndex >= totalGroups) ? Collections.emptyList() : filteredList.subList(startIndex, endIndex);

        // 4. Mappen naar DTO
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

            // Gebruik jouw getters!
            if (detail.getExternalMembers() > 0 || detail.isExternalAllowed()) {
                groupsWithExternal++;
            }

            switch (detail.getRisk()) {
                case "HIGH" -> highRiskGroups++;
                case "MEDIUM" -> mediumRiskGroups++;
                default -> lowRiskGroups++;
            }
        }

        int securityScore = totalGroups == 0 ? 0
                : (int) Math.round((lowRiskGroups * 100.0 + mediumRiskGroups * 60.0 + highRiskGroups * 20.0) / totalGroups);

        return new GroupOverviewResponse(
                totalGroups, groupsWithExternal, highRiskGroups, mediumRiskGroups, lowRiskGroups, securityScore
        );
    }
}
