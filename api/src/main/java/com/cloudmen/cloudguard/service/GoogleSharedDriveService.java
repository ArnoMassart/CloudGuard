package com.cloudmen.cloudguard.service;

import com.cloudmen.cloudguard.dto.drives.SharedDriveBasicDetail;
import com.cloudmen.cloudguard.dto.drives.SharedDriveOverviewResponse;
import com.cloudmen.cloudguard.dto.drives.SharedDrivePageResponse;
import com.cloudmen.cloudguard.dto.drives.SharedDriveSecurityData;
import com.cloudmen.cloudguard.utility.DateTimeConverter;
import com.cloudmen.cloudguard.utility.GoogleApiFactory;
import com.google.api.services.drive.Drive;
import com.google.api.services.drive.DriveScopes;
import com.google.api.services.drive.model.DriveList;
import com.google.api.services.drive.model.Permission;
import com.google.api.services.drive.model.PermissionList;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

@Service
public class GoogleSharedDriveService {
    private final GoogleApiFactory googleApiFactory;

    public GoogleSharedDriveService(GoogleApiFactory googleApiFactory) {
        this.googleApiFactory = googleApiFactory;
    }

    public SharedDrivePageResponse getSharedDrivesPaged(String loggedInEmail, String pageToken, int size, String query) {
        try {
            Drive driveService = googleApiFactory.getDriveService(DriveScopes.DRIVE_READONLY, loggedInEmail);
            String customerDomain = loggedInEmail.substring(loggedInEmail.indexOf("@") + 1);

            Drive.Drives.List request = driveService.drives().list()
                    .setUseDomainAdminAccess(true)
                    .setPageSize(size)
                    .setPageToken(pageToken)
                    .setFields("nextPageToken, drives(id, name, createdTime, restrictions)");

            if (query != null && !query.trim().isEmpty()) {
                String safeQuery = query.trim().replace("'", "\\'");
                request.setQ("name contains '" + safeQuery + "'");
            }

            DriveList driveList = request.execute();

            if (driveList.getDrives() == null) {
                return new SharedDrivePageResponse(Collections.emptyList(), null);
            }

            List<SharedDriveBasicDetail> mappedDrives = driveList.getDrives().stream().map(drive -> {
                com.google.api.services.drive.model.Drive.Restrictions res = drive.getRestrictions();
                boolean domainOnly = res != null && Boolean.TRUE.equals(res.getDomainUsersOnly());
                boolean membersOnly = res != null && Boolean.TRUE.equals(res.getDriveMembersOnly());

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
                    System.err.println("Kon leden niet laden voor drive: " + drive.getId());
                }

                String createdTime = drive.getCreatedTime() != null ? DateTimeConverter.convertToTimeAgo(drive.getCreatedTime()) : "Onbekend";

                String risk = calculateRisk(externalMembers, totalOrganizers ,domainOnly, membersOnly);

                return new SharedDriveBasicDetail(
                        drive.getId(),
                        drive.getName(),
                        totalMembers,
                        externalMembers,
                        totalOrganizers,
                        createdTime,
                        domainOnly,
                        membersOnly,
                        risk
                );
            }).toList();

            return new SharedDrivePageResponse(mappedDrives, driveList.getNextPageToken());

        } catch (Exception e) {
            throw new RuntimeException("Fout bij ophalen Shared Drives: " + e.getMessage());
        }
    }

    public SharedDriveOverviewResponse getDrivesPageOverview(String loggedInEmail) {
        try {
            List<SharedDriveSecurityData> drives = fetchAllDrivesWithSecurityData(loggedInEmail);

            long totalDrives = drives.size();
            long orphanDrives = drives.stream().filter(d -> d.organizerCount() <= 0).count();
            long totalLowRisk = drives.stream().filter(d -> d.risk().equals("Laag")).count();
            long totalMediumRisk = drives.stream().filter(d -> d.risk().equals("Middel")).count();
            long totalHighRisk = drives.stream().filter(d -> d.risk().equals("Hoog")).count();
            long totalExternalMembersCount = drives.stream().filter(d -> d.externalMemberCount() > 0).count();
            long securityScore = totalDrives == 0 ? 0 : (int) Math.round((totalLowRisk * 100.0 + totalMediumRisk * 60.0 + totalHighRisk * 20.0) / totalDrives);
            long notOnlyDomainUsersAllowedCount = drives.stream().filter(d -> !d.domainUsersOnly()).count();
            long notOnlyMembersCanAccessCount = drives.stream().filter(d -> !d.driveMembersOnly()).count();
            long externalMembersDriveCount = drives.stream().filter(d -> d.externalMemberCount() > 0).count();

            return new SharedDriveOverviewResponse(
                    totalDrives,
                    orphanDrives,
                    totalHighRisk,
                    totalExternalMembersCount,
                    securityScore,
                    notOnlyDomainUsersAllowedCount,
                    notOnlyMembersCanAccessCount,
                    externalMembersDriveCount
            );
        } catch (Exception e) {
            throw new RuntimeException("Failed to fetch drives overview from Google: " + e.getMessage());
        }
    }

    private List<SharedDriveSecurityData> fetchAllDrivesWithSecurityData(String loggedInEmail) {
        List<SharedDriveSecurityData> allDrivesData = new ArrayList<>();

        try {
            Drive driveService = googleApiFactory.getDriveService(DriveScopes.DRIVE_READONLY, loggedInEmail);
            String customerDomain = loggedInEmail.substring(loggedInEmail.indexOf("@") + 1);

            String pageToken = null;

            // 1. Haal alle drives op via de interne Google paginatie
            do {
                DriveList result = driveService.drives().list()
                        .setUseDomainAdminAccess(true)
                        .setPageToken(pageToken)
                        .setFields("nextPageToken, drives(id, name, restrictions)")
                        .execute();

                if (result.getDrives() != null) {
                    for (com.google.api.services.drive.model.Drive drive : result.getDrives()) {

                        // 2. Lees de restricties uit
                        com.google.api.services.drive.model.Drive.Restrictions res = drive.getRestrictions();
                        boolean domainOnly = res != null && Boolean.TRUE.equals(res.getDomainUsersOnly());
                        boolean membersOnly = res != null && Boolean.TRUE.equals(res.getDriveMembersOnly());

                        int externalMembers = 0;
                        int organizers = 0;

                        // 3. Haal de permissies (leden) op voor deze specifieke drive
                        try {
                            PermissionList permissions = driveService.permissions().list(drive.getId())
                                    .setSupportsAllDrives(true)
                                    .setUseDomainAdminAccess(true)
                                    .setFields("permissions(id, emailAddress, role)")
                                    .execute();

                            if (permissions.getPermissions() != null) {
                                for (Permission p : permissions.getPermissions()) {
                                    // Tel externe leden
                                    if (p.getEmailAddress() != null && !p.getEmailAddress().endsWith("@" + customerDomain)) {
                                        externalMembers++;
                                    }
                                    // Tel beheerders (voor wees-drives)
                                    if ("organizer".equals(p.getRole())) {
                                        organizers++;
                                    }
                                }
                            }
                        } catch (Exception e) {
                            System.err.println("Kon leden niet laden voor drive: " + drive.getName());
                        }

                        String risk = calculateRisk(externalMembers, organizers , domainOnly, membersOnly);

                        // 4. Voeg toe aan de totaal-lijst
                        allDrivesData.add(new SharedDriveSecurityData(
                                drive.getId(),
                                drive.getName(),
                                domainOnly,
                                membersOnly,
                                externalMembers,
                                organizers,
                                risk
                        ));
                    }
                }
                pageToken = result.getNextPageToken();
            } while (pageToken != null);

            return allDrivesData;

        } catch (Exception e) {
            throw new RuntimeException("Fout bij ophalen van alle Shared Drives: " + e.getMessage());
        }
    }

    private String calculateRisk(int externalMembers, int totalOrganizers, boolean domainOnly, boolean membersOnly) {
        int riskCount = 0;

        if (externalMembers > 0) riskCount++;
        if (!domainOnly) riskCount++;
        if (!membersOnly) riskCount++;
        if (totalOrganizers <= 0) riskCount++;

        switch (riskCount) {
            case 1, 2 -> { return "Middel";}
            case 3, 4 -> {
                return "Hoog";
            }
            default -> {
                return "Laag";
            }
        }
    }
}
