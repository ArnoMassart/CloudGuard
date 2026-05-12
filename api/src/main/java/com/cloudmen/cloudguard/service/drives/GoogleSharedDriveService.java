package com.cloudmen.cloudguard.service.drives;

import com.cloudmen.cloudguard.dto.devices.DeviceOverviewResponse;
import com.cloudmen.cloudguard.dto.drives.SharedDriveBasicDetail;
import com.cloudmen.cloudguard.dto.drives.SharedDriveCacheEntry;
import com.cloudmen.cloudguard.dto.drives.SharedDriveOverviewResponse;
import com.cloudmen.cloudguard.dto.drives.SharedDrivePageResponse;
import com.cloudmen.cloudguard.dto.password.SecurityScoreBreakdownDto;
import com.cloudmen.cloudguard.dto.password.SecurityScoreFactorDto;
import com.cloudmen.cloudguard.dto.preferences.SectionWarningsDto;
import com.cloudmen.cloudguard.service.cache.GoogleSharedDriveCacheService;
import com.cloudmen.cloudguard.service.preference.SecurityPreferenceScoreSupport;
import com.cloudmen.cloudguard.service.preference.SectionWarningEvaluator;
import com.cloudmen.cloudguard.utility.DateTimeConverter;
import com.cloudmen.cloudguard.utility.GoogleServiceHelperMethods;

import static com.cloudmen.cloudguard.utility.GoogleServiceHelperMethods.securityScoreFactorForDetail;
import org.springframework.context.MessageSource;
import org.springframework.context.i18n.LocaleContextHolder;
import org.springframework.stereotype.Service;

import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.Locale;

import static com.cloudmen.cloudguard.utility.GoogleServiceHelperMethods.severity;

/**
 * The primary orchestration service for managing and analyzing Google Workspace Shared Drives. <p>
 *
 * This service acts as a facade, handling the retrieval, filtering, and pagination of shared drive data from the
 * cache. To adhere to the Single Responsibility Principle, it delegates complex risk analysis, preference handling,
 * and compliance scoring to the {@link DrivesComplianceScorer}.
 */
@Service
public class GoogleSharedDriveService {
    private final GoogleSharedDriveCacheService sharedDriveCacheService;
    private final DrivesComplianceScorer drivesComplianceScorer;

    public GoogleSharedDriveService(GoogleSharedDriveCacheService sharedDriveCacheService, DrivesComplianceScorer drivesComplianceScorer) {
        this.sharedDriveCacheService = sharedDriveCacheService;
        this.drivesComplianceScorer = drivesComplianceScorer;
    }

    /**
     * Triggers a manual background refresh of the Shared Drive cache for the specified user.
     *
     * @param loggedInEmail the email of the authenticated user
     */
    public void forceRefreshCache(String loggedInEmail) {
        sharedDriveCacheService.forceRefreshCache(loggedInEmail);
    }

    /**
     * Retrieves a paginated and optionally filtered list of Shared Drives. <p>
     *
     * This method fetches data from the cache, applies a case-insensitive text filter on the drive name, and
     * partitions the result. It also formats the creation timestamps into human-readable "time ago" strings.
     *
     * @param loggedInEmail the email of the authenticated user
     * @param pageToken     the string representation of the requested page index
     * @param size          the maximum number of drives to return per page
     * @param query         an optional search string to filter drives by name
     * @return a {@link SharedDrivePageResponse} containing the requested page and metadata
     */
    public SharedDrivePageResponse getSharedDrivesPaged(String loggedInEmail, String pageToken, int size, String query) {
        SharedDriveCacheEntry cachedData = sharedDriveCacheService.getOrFetchData(loggedInEmail);

        List<SharedDriveBasicDetail> filteredList = cachedData.allDrives();
        if (query != null && !query.trim().isEmpty()) {
            String lowerQuery = query.toLowerCase().trim();
            filteredList = filteredList.stream()
                    .filter(d -> d.name() != null && d.name().toLowerCase().contains(lowerQuery))
                    .toList();
        }

        int page = GoogleServiceHelperMethods.getPage(pageToken);

        int totalDrives = filteredList.size();
        int startIndex = (page - 1) * size;
        int endIndex = Math.min(startIndex + size, totalDrives);

        List<SharedDriveBasicDetail> pagedItems = (startIndex >= totalDrives)
                ? Collections.emptyList()
                : filteredList.subList(startIndex, endIndex).stream().map(item -> {
            String time = DateTimeConverter.convertToTimeAgo(item.createdTime());
            return new SharedDriveBasicDetail(
                    item.id(),
                    item.name(),
                    item.totalMembers(),
                    item.externalMembers(),
                    item.totalOrganizers(),
                    item.createdTime(),
                    time,
                    item.onlyDomainUsersAllowed(),
                    item.onlyMembersCanAccess(),
                    item.risk()
            );
        }).toList();

        String nextTokenToReturn = (endIndex < totalDrives) ? String.valueOf(page + 1) : null;
        return new SharedDrivePageResponse(pagedItems, nextTokenToReturn);
    }

    /**
     * Retrieves a comprehensive security and compliance overview of all Shared Drives within the organization. <p>
     *
     * This method aggregates risk metrics (such as orphan drives, external members, and permissive sharing settings)
     * and utilizes the {@link DrivesComplianceScorer} to generate a preference-adjusted security score and breakdown.
     *
     * @param loggedInEmail the email of the authenticated user
     * @param disabledKeys  a set of security check keys to ignore based on user preferences
     * @return a {@link SharedDriveOverviewResponse} containing aggregated metrics, scores, and warnings
     */
    public SharedDriveOverviewResponse getDrivesPageOverview(String loggedInEmail, Set<String> disabledKeys) {
        Set<String> off = disabledKeys == null ? Set.of() : disabledKeys;
        SharedDriveCacheEntry cachedData = sharedDriveCacheService.getOrFetchData(loggedInEmail);
        List<SharedDriveBasicDetail> drives = cachedData.allDrives();

        int totalDrives = drives.size();

        if (totalDrives == 0) {
            SectionWarningsDto warnings = SectionWarningEvaluator.with(off)
                    .check("notOnlyDomainUsersAllowedWarning", 0, "shared-drives", "outsideDomain")
                    .check("notOnlyMembersCanAccessWarning", 0, "shared-drives", "nonMemberAccess")
                    .check("externalMembersWarning", 0, "shared-drives", "external")
                    .check("orphanDrivesWarning", 0, "shared-drives", "orphan")
                    .build();
            return new SharedDriveOverviewResponse(
                    0, 0, 0, 0,
                    null, 0, 0, 0,
                    null,
                    warnings
            );
        }

        int orphanDrives = (int) drives.stream().filter(d -> d.totalOrganizers() <= 0).count();
        int totalLowRisk = (int) drives.stream().filter(d -> d.risk().equals("low")).count();
        int totalMediumRisk = (int) drives.stream().filter(d -> d.risk().equals("middle")).count();
        int totalHighRisk = (int) drives.stream().filter(d -> d.risk().equals("high")).count();
        int totalExternalMembersCount = (int) drives.stream().filter(d -> d.externalMembers() > 0).count();

        int riskOnlyScore = (int) Math.round((totalLowRisk * 100.0 + totalMediumRisk * 60.0 + totalHighRisk * 20.0) / totalDrives);

        int notOnlyDomainUsersAllowedCount = (int) drives.stream().filter(d -> !d.onlyDomainUsersAllowed()).count();
        int notOnlyMembersCanAccessCount = (int) drives.stream().filter(d -> !d.onlyMembersCanAccess()).count();
        int externalMembersDriveCount = (int) drives.stream().filter(d -> d.externalMembers() > 0).count();

        SecurityScoreBreakdownDto breakdown = drivesComplianceScorer.buildDrivesBreakdown(
                totalDrives, totalLowRisk, totalMediumRisk, totalHighRisk, orphanDrives,
                notOnlyDomainUsersAllowedCount, notOnlyMembersCanAccessCount,
                riskOnlyScore);

        SectionWarningsDto warnings = SectionWarningEvaluator.with(off)
                .check("notOnlyDomainUsersAllowedWarning", notOnlyDomainUsersAllowedCount, "shared-drives", "outsideDomain")
                .check("notOnlyMembersCanAccessWarning", notOnlyMembersCanAccessCount, "shared-drives", "nonMemberAccess")
                .check("externalMembersWarning", externalMembersDriveCount, "shared-drives", "external")
                .check("orphanDrivesWarning", orphanDrives, "shared-drives", "orphan")
                .build();

        return new SharedDriveOverviewResponse(
                totalDrives,
                orphanDrives,
                totalHighRisk,
                totalExternalMembersCount,
                breakdown.totalScore(),
                notOnlyDomainUsersAllowedCount,
                notOnlyMembersCanAccessCount,
                externalMembersDriveCount,
                breakdown,
                warnings
        );
    }
}
