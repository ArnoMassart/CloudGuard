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

    public GroupSettingsDto getGroupSettings(String loggedInEmail, String groupEmail) {
        if (groupEmail == null || groupEmail.isBlank()) {
            return new GroupSettingsDto("—", "—");
        }
        try {
            Groupssettings settingsService = groupsCacheService.getGroupsSettingsService(loggedInEmail);
            Groups settings = settingsService.groups().get(groupEmail).execute();
            String whoCanJoin = mapWhoCanJoin(settings.getWhoCanJoin());
            String whoCanView = mapWhoCanViewMembership(settings.getWhoCanViewMembership());
            return new GroupSettingsDto(whoCanJoin, whoCanView);
        } catch (Throwable t) {
            log.warn("Could not fetch Groups Settings for {}: {}", groupEmail, t.getMessage());
            return new GroupSettingsDto("—", "—");
        }
    }

    private String mapWhoCanJoin(String who) {
        if (who == null || who.isBlank()) return "—";
        return switch (who) {
            case "ANYONE_CAN_JOIN" -> "Iedereen kan lid worden";
            case "INVITED_CAN_JOIN" -> "Alleen uitgenodigde gebruikers";
            case "CAN_REQUEST_TO_JOIN" -> "Kan verzoek doen om lid te worden";
            case "ALL_IN_DOMAIN_CAN_JOIN" -> "Iedereen in het domein";
            default -> who;
        };
    }

    private String mapWhoCanViewMembership(String who) {
        if (who == null || who.isBlank()) return "—";
        return switch (who) {
            case "ALL_IN_DOMAIN_CAN_VIEW" -> "Iedereen in het domein";
            case "ALL_MANAGERS_CAN_VIEW" -> "Alle beheerders";
            case "ALL_MEMBERS_CAN_VIEW" -> "Alle leden";
            default -> who;
        };
    }
}
