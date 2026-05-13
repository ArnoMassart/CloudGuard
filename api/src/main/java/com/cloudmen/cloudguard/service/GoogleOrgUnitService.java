package com.cloudmen.cloudguard.service;

import com.cloudmen.cloudguard.dto.organization.OrgUnitCacheEntry;
import com.cloudmen.cloudguard.dto.organization.OrgUnitNodeDto;
import com.cloudmen.cloudguard.service.cache.GoogleOrgUnitCacheService;
import com.google.api.services.admin.directory.model.OrgUnit;
import org.springframework.context.MessageSource;
import org.springframework.context.i18n.LocaleContextHolder;
import org.springframework.stereotype.Service;

import java.util.*;

/**
 * Builds a navigable <strong>organizational unit tree</strong> for the UI from {@link OrgUnitCacheEntry}: synthetic
 * domain root at {@code "/"}, children matched via Admin SDK {@link OrgUnit#getParentOrgUnitPath()}, sorted display
 * names, and user totals keyed by {@link OrgUnit#getOrgUnitPath()}.
 */
@Service
public class GoogleOrgUnitService {

    private final GoogleOrgUnitCacheService orgUnitCacheService;
    private final MessageSource messageSource;

    /**
     * @param orgUnitCacheService Directory-backed OU list and user counts per path
     * @param messageSource       bundle keys such as {@code orgUnits.empty_root}
     */
    public GoogleOrgUnitService(GoogleOrgUnitCacheService orgUnitCacheService, MessageSource messageSource) {
        this.orgUnitCacheService = orgUnitCacheService;
        this.messageSource = messageSource;
    }

    /** Triggers a synchronous refresh of the tenant’s OU cache entry. */
    public void forceRefreshCache(String loggedInEmail) {
        orgUnitCacheService.forceRefreshCache(loggedInEmail);
    }

    /**
     * Returns the OU hierarchy for the authenticated user’s workspace. When Google returns no units, yields a single
     * root node with an i18n placeholder name.
     *
     * @param loggedInEmail user email; primary domain for the synthetic root label is taken from the part after {@code @}
     */
    public OrgUnitNodeDto getOrgUnitTree(String loggedInEmail) {
        OrgUnitCacheEntry cachedData = orgUnitCacheService.getOrFetchData(loggedInEmail);
        List<OrgUnit> allOrgUnits = cachedData.allOrgUnits();
        Map<String, Integer> userCounts = cachedData.userCounts();

        if (allOrgUnits == null || allOrgUnits.isEmpty()) {
            return buildEmptyRoot(messageSource);
        }

        String domain = loggedInEmail.substring(loggedInEmail.indexOf('@') + 1);
        return buildTree(allOrgUnits, domain, userCounts);
    }

    /** Domain root plus recursive children; {@code id} is the OU path string. */
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

    /** Direct children of {@code parentPath}, excluding self-references and paths not under the expected parent. */
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
