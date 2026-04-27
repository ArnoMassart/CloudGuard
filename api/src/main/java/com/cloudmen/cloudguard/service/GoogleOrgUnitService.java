package com.cloudmen.cloudguard.service;

import com.cloudmen.cloudguard.dto.organization.OrgUnitCacheEntry;
import com.cloudmen.cloudguard.dto.organization.OrgUnitNodeDto;
import com.cloudmen.cloudguard.service.cache.GoogleOrgUnitCacheService;
import com.google.api.services.admin.directory.model.OrgUnit;
import org.springframework.context.MessageSource;
import org.springframework.context.i18n.LocaleContextHolder;
import org.springframework.stereotype.Service;

import java.util.*;

@Service
public class GoogleOrgUnitService { 

    private final GoogleOrgUnitCacheService orgUnitCacheService;
    private final MessageSource messageSource;

    public GoogleOrgUnitService(GoogleOrgUnitCacheService orgUnitCacheService, MessageSource messageSource) {
        this.orgUnitCacheService = orgUnitCacheService;
        this.messageSource = messageSource;
    }

    public void forceRefreshCache(String adminEmail) {
        orgUnitCacheService.forceRefreshCache(adminEmail);
    }

    public OrgUnitNodeDto getOrgUnitTree(String loggedInEmail) {
        OrgUnitCacheEntry cachedData = orgUnitCacheService.getOrFetchOrgUnitData(loggedInEmail);
        List<OrgUnit> allOrgUnits = cachedData.allOrgUnits();
        Map<String, Integer> userCounts = cachedData.userCounts();

        if (allOrgUnits == null || allOrgUnits.isEmpty()) {
            return buildEmptyRoot(messageSource);
        }

        // 2. Bouw de boom
        String domain = loggedInEmail.substring(loggedInEmail.indexOf('@') + 1);
        return buildTree(allOrgUnits, domain, userCounts);
    }

    private OrgUnitNodeDto buildTree(List<OrgUnit> allOrgUnits, String domain, Map<String, Integer> userCounts) {
        OrgUnitNodeDto root = new OrgUnitNodeDto();
        root.setId("/");
        root.setName(domain);
        root.setOrgUnitPath("/");
        root.setRoot(true);
        root.setUserCount(userCounts.getOrDefault("/", 0));
        root.setChildren(buildChildren("/", allOrgUnits, userCounts));
        return root;
    }

    private List<OrgUnitNodeDto> buildChildren(String parentPath, List<OrgUnit> allOrgUnits, Map<String, Integer> userCounts) {
        List<OrgUnitNodeDto> children = new ArrayList<>();
        String normParent = normalizePath(parentPath);

        for (OrgUnit ou : allOrgUnits) {
            String ouPath = ou.getOrgUnitPath();
            String parent = ou.getParentOrgUnitPath();
            if (ouPath == null || ouPath.isBlank()) continue;

            if (normalizePath(ouPath).equals(normParent)) continue;
            if (!isChildOf(parent, normParent)) continue;

            OrgUnitNodeDto dto = toDto(ou, false);
            dto.setUserCount(userCounts.getOrDefault(dto.getOrgUnitPath(), 0));
            dto.setChildren(buildChildren(dto.getOrgUnitPath(), allOrgUnits, userCounts));
            children.add(dto);
        }
        children.sort(Comparator.comparing(OrgUnitNodeDto::getName, Comparator.nullsFirst(String.CASE_INSENSITIVE_ORDER)));
        return children;
    }

    private OrgUnitNodeDto toDto(OrgUnit ou, boolean isRoot) {
        String path = ou.getOrgUnitPath() != null ? ou.getOrgUnitPath() : "/";
        String name = ou.getName() != null && !ou.getName().isBlank()
                ? ou.getName()
                : path.substring(path.lastIndexOf('/') + 1);
        if (name.isBlank()) name = path;

        OrgUnitNodeDto dto = new OrgUnitNodeDto();
        dto.setId(path);
        dto.setName(name);
        dto.setOrgUnitPath(path);
        dto.setRoot(isRoot);
        dto.setUserCount(0);
        dto.setChildren(new ArrayList<>());
        return dto;
    }

    private String normalizePath(String path) {
        if (path == null || path.isBlank()) return "/";
        return path.trim();
    }

    private boolean isChildOf(String parent, String expectedParent) {
        if (parent == null) return "/".equals(expectedParent);
        String norm = parent.trim();
        return norm.equals(expectedParent) || (norm.isEmpty() && "/".equals(expectedParent));
    }

    private OrgUnitNodeDto buildEmptyRoot(MessageSource messageSource) {
        OrgUnitNodeDto root = new OrgUnitNodeDto();
        root.setId("/");

        Locale locale = LocaleContextHolder.getLocale();

        String name = messageSource.getMessage("orgUnits.empty_root", null, locale);
        root.setName(name);
        root.setOrgUnitPath("/");
        root.setRoot(true);
        root.setUserCount(0);
        root.setChildren(new ArrayList<>());
        return root;
    }
}
