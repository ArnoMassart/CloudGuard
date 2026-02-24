package com.cloudmen.cloudguard.service;

import com.cloudmen.cloudguard.dto.organization.OrgUnitNodeDto;
import com.cloudmen.cloudguard.utility.GoogleApiFactory;
import com.google.api.services.admin.directory.Directory;
import com.google.api.services.admin.directory.DirectoryScopes;
import com.google.api.services.admin.directory.model.OrgUnit;
import com.google.api.services.admin.directory.model.OrgUnits;
import com.google.api.services.admin.directory.model.Users;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Set;

@Service
public class GoogleOrgUnitService { 

    private static final Logger log = LoggerFactory.getLogger(GoogleOrgUnitService.class);

    private final GoogleApiFactory directoryFactory;

    public GoogleOrgUnitService(GoogleApiFactory directoryFactory) {
        this.directoryFactory = directoryFactory;
    }

    public OrgUnitNodeDto getOrgUnitTree(String loggedInEmail) {
        try {
            Set<String> scopes = Set.of(
                    DirectoryScopes.ADMIN_DIRECTORY_ORGUNIT_READONLY,
                    DirectoryScopes.ADMIN_DIRECTORY_USER_READONLY
            );
            Directory directory = directoryFactory.getDirectoryService(scopes, loggedInEmail);

            List<OrgUnit> allOrgUnits = fetchAllOrgUnits(directory);

            if (allOrgUnits == null || allOrgUnits.isEmpty()) {
                return buildEmptyRoot();
            }

            String domain = loggedInEmail.substring(loggedInEmail.indexOf('@') + 1);
            return buildTree(directory, allOrgUnits, domain);
        } catch (Exception e) {
            log.error("Failed to fetch org unit tree: {}", e.getMessage(), e);
            return buildEmptyRoot();
        }
    }

    private List<OrgUnit> fetchAllOrgUnits(Directory directory) throws IOException {
        OrgUnits response = directory.orgunits()
                .list("my_customer")
                .setType("all")
                .execute();

        if (response == null || response.getOrganizationUnits() == null) {
            return new ArrayList<>();
        }
        return response.getOrganizationUnits();
    }

    private OrgUnitNodeDto buildTree(Directory directory, List<OrgUnit> allOrgUnits, String domain) throws IOException {
        OrgUnitNodeDto root = new OrgUnitNodeDto();
        root.setId("/");
        root.setName(domain);
        root.setOrgUnitPath("/");
        root.setRoot(true);
        root.setUserCount(countUsersInOrgUnit(directory, "/"));
        root.setChildren(buildChildren(directory, "/", allOrgUnits));
        return root;
    }

    private List<OrgUnitNodeDto> buildChildren(Directory directory, String parentPath, List<OrgUnit> allOrgUnits) throws IOException {
        List<OrgUnitNodeDto> children = new ArrayList<>();
        String normParent = normalizePath(parentPath);

        for (OrgUnit ou : allOrgUnits) {
            String ouPath = ou.getOrgUnitPath();
            String parent = ou.getParentOrgUnitPath();
            if (ouPath == null || ouPath.isBlank()) continue;

            if (normalizePath(ouPath).equals(normParent)) continue;
            if (!isChildOf(parent, normParent)) continue;

            OrgUnitNodeDto dto = toDto(ou, false);
            dto.setUserCount(countUsersInOrgUnit(directory, dto.getOrgUnitPath()));
            dto.setChildren(buildChildren(directory, dto.getOrgUnitPath(), allOrgUnits));
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

    private int countUsersInOrgUnit(Directory directory, String orgUnitPath) {
        if (orgUnitPath == null || orgUnitPath.isBlank()) return 0;
        try {
            int count = 0;
            String pageToken = null;
            String query = "orgUnitPath='" + orgUnitPath.replace("'", "\\'") + "'";

            do {
                Directory.Users.List request = directory.users().list()
                        .setCustomer("my_customer")
                        .setMaxResults(500)
                        .setQuery(query);
                if (pageToken != null && !pageToken.isBlank()) {
                    request.setPageToken(pageToken);
                }
                Users response = request.execute();
                if (response.getUsers() != null) {
                    count += response.getUsers().size();
                }
                pageToken = response.getNextPageToken();
            } while (pageToken != null && !pageToken.isBlank());

            return count;
        } catch (Exception e) {
            log.warn("Could not count users for org unit {}: {}", orgUnitPath, e.getMessage());
            return 0;
        }
    }

    private static String normalizePath(String path) {
        if (path == null || path.isBlank()) return "/";
        return path.trim();
    }

    private static boolean isChildOf(String parent, String expectedParent) {
        if (parent == null) return "/".equals(expectedParent);
        String norm = parent.trim();
        return norm.equals(expectedParent) || (norm.isEmpty() && "/".equals(expectedParent));
    }

    private static OrgUnitNodeDto buildEmptyRoot() {
        OrgUnitNodeDto root = new OrgUnitNodeDto();
        root.setId("/");
        root.setName("Organisatie");
        root.setOrgUnitPath("/");
        root.setRoot(true);
        root.setUserCount(0);
        root.setChildren(new ArrayList<>());
        return root;
    }

}
