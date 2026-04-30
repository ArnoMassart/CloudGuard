package com.cloudmen.cloudguard.service.cache;

import com.cloudmen.cloudguard.dto.drives.DriveMetrics;
import com.cloudmen.cloudguard.dto.drives.SharedDriveBasicDetail;
import com.cloudmen.cloudguard.dto.drives.SharedDriveCacheEntry;
import com.cloudmen.cloudguard.exception.GoogleWorkspaceSyncException;
import com.cloudmen.cloudguard.service.OrganizationService;
import com.cloudmen.cloudguard.service.user.UserService;
import com.cloudmen.cloudguard.utility.GoogleApiFactory;
import com.google.api.services.drive.Drive;
import com.google.api.services.drive.DriveScopes;
import com.google.api.services.drive.model.DriveList;
import com.google.api.services.drive.model.Permission;
import com.google.api.services.drive.model.PermissionList;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

/**
 * Service responsible for retrieving, assessing, and caching Google Workspace Shared Drive data. <p>
 *
 * This service fetches all Shared Drives for a domain, evaluates their sharing restrictions, and iterates through their
 * permissions to identify external members and active managers (organizers). It then calculates a security risk score
 * for each drive based on its exposure.
 */
@Service
public class GoogleSharedDriveCacheService extends AbstractGoogleWorkspaceCacheService<SharedDriveCacheEntry> {
    private static final Logger log = LoggerFactory.getLogger(GoogleSharedDriveCacheService.class);

    private final GoogleApiFactory googleApiFactory;

    public GoogleSharedDriveCacheService(GoogleApiFactory googleApiFactory, UserService userService, OrganizationService organizationService) {
        super(userService, organizationService, 24);
        this.googleApiFactory = googleApiFactory;
    }

    @Override
    protected SharedDriveCacheEntry fetchFromGoogle(String adminEmail, SharedDriveCacheEntry fallbackEntry) {
        try {
            log.info("Ophalen LIVE Shared Drive data van Google. Impersonatie via Admin: {}", adminEmail);

            Drive driveService = googleApiFactory.getDriveService(DriveScopes.DRIVE_READONLY, adminEmail);
            String customerDomain = adminEmail.substring(adminEmail.indexOf("@") + 1);

            List<SharedDriveBasicDetail> allDrives = fetchAllDrivesWithSecurityData(driveService, customerDomain);

            return new SharedDriveCacheEntry(allDrives, System.currentTimeMillis());

        } catch (Exception e) {
            if (fallbackEntry != null) {
                log.error("Google API faalde! Terugvallen op oude cache: {}", e.getMessage());
                return fallbackEntry;
            }
            throw new GoogleWorkspaceSyncException("Fout bij ophalen Google Shared Drives, en geen cache beschikbaar: " + e.getMessage());
        }
    }

    private List<SharedDriveBasicDetail> fetchAllDrivesWithSecurityData(Drive driveService, String customerDomain) {
        List<SharedDriveBasicDetail> allDrivesData = new ArrayList<>();
        try {
            String pageToken = null;

            do {
                DriveList result = driveService.drives().list()
                        .setUseDomainAdminAccess(true)
                        .setPageToken(pageToken)
                        .setFields("nextPageToken, drives(id, name, createdTime, restrictions)")
                        .execute();

                if (result.getDrives() != null) {
                    for (com.google.api.services.drive.model.Drive drive : result.getDrives()) {
                        allDrivesData.add(processSingleDrive(driveService, drive, customerDomain));
                    }
                }
                pageToken = result.getNextPageToken();
            } while (pageToken != null);

            return allDrivesData;

        } catch (Exception e) {
            throw new GoogleWorkspaceSyncException("Fout bij ophalen van alle Shared Drives: " + e.getMessage());
        }
    }

    private SharedDriveBasicDetail processSingleDrive(Drive driveService, com.google.api.services.drive.model.Drive drive, String customerDomain) {
        com.google.api.services.drive.model.Drive.Restrictions res = drive.getRestrictions();
        boolean domainOnly = res != null && Boolean.TRUE.equals(res.getDomainUsersOnly());
        boolean membersOnly = res != null && Boolean.TRUE.equals(res.getDriveMembersOnly());

        DriveMetrics metrics = fetchDriveMetrics(driveService, drive, customerDomain);

        String risk = calculateRisk(metrics.externalMembers(), metrics.totalOrganizers(), domainOnly, membersOnly);

        return new SharedDriveBasicDetail(
                drive.getId(),
                drive.getName(),
                metrics.totalMembers(),
                metrics.externalMembers(),
                metrics.totalOrganizers(),
                drive.getCreatedTime(),
                "",
                domainOnly,
                membersOnly,
                risk
        );
    }

    private DriveMetrics fetchDriveMetrics(Drive driveService, com.google.api.services.drive.model.Drive drive, String customerDomain) {
        int totalMembers = 0;
        int externalMembers = 0;
        int totalOrganizers = 0;

        try {
            PermissionList permissions = driveService.permissions().list(drive.getId())
                    .setSupportsAllDrives(true)
                    .setUseDomainAdminAccess(true)
                    .setFields("permissions(id, emailAddress, role)")
                    .execute();

            if (permissions.getPermissions() != null) {
                totalMembers = permissions.getPermissions().size();
                for (Permission p : permissions.getPermissions()) {
                    if (p.getEmailAddress() != null && !p.getEmailAddress().endsWith("@" + customerDomain)) {
                        externalMembers++;
                    }
                    if ("organizer".equals(p.getRole())) {
                        totalOrganizers++;
                    }
                }
            }
        } catch (Exception e) {
            log.warn("Kon leden niet laden voor drive: {}", drive.getName());
        }

        return new DriveMetrics(totalMembers, externalMembers, totalOrganizers);
    }

    private String calculateRisk(int externalMembers, int totalOrganizers, boolean domainOnly, boolean membersOnly) {
        int riskCount = 0;

        if (externalMembers > 0) riskCount++;
        if (!domainOnly) riskCount++;
        if (!membersOnly) riskCount++;
        if (totalOrganizers <= 0) riskCount++;

        switch (riskCount) {
            case 1, 2 -> { return "middle";}
            case 3, 4 -> {
                return "high";
            }
            default -> {
                return "low";
            }
        }
    }
}
