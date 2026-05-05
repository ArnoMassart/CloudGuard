package com.cloudmen.cloudguard.service.device;

import com.cloudmen.cloudguard.dto.devices.*;
import com.cloudmen.cloudguard.dto.password.SecurityScoreBreakdownDto;
import com.cloudmen.cloudguard.dto.preferences.SectionWarningsDto;
import com.cloudmen.cloudguard.service.cache.GoogleDeviceCacheService;
import com.cloudmen.cloudguard.service.preference.SecurityPreferenceScoreSupport;
import com.cloudmen.cloudguard.service.preference.SectionWarningEvaluator;
import com.cloudmen.cloudguard.utility.GoogleServiceHelperMethods;
import org.springframework.stereotype.Service;

import java.util.*;

/**
 * Service responsible for managing and aggregating hardware device data from various Google Workspace sources. <p>
 *
 * This service acts as a funnel, transforming heterogeneous device types (Mobile, ChromeOS, and Endpoint/GCPW) into a
 * unified {@link DeviceDetail} model. It calculates security compliance scores based on factors like encryption and
 * screen locks, while respecting organization-specific security preferences that may exclude certain checks.
 */
@Service
public class GoogleDeviceService {
    private final GoogleDeviceCacheService deviceCacheService;
    private final GoogleDeviceMapper mapper;
    private final DeviceComplianceScorer deviceComplianceScorer;

    public GoogleDeviceService(GoogleDeviceCacheService deviceCacheService, GoogleDeviceMapper mapper, DeviceComplianceScorer deviceComplianceScorer) {
        this.deviceCacheService = deviceCacheService;
        this.mapper = mapper;
        this.deviceComplianceScorer = deviceComplianceScorer;
    }

    /**
     * Triggers a manual refresh of the device cache for a specific user.
     *
     * @param loggedInEmail email of the user triggering the refresh
     */
    public void forceRefreshCache(String loggedInEmail) {
        deviceCacheService.forceRefreshCache(loggedInEmail);
    }

    /**
     * Retrieves a paginated and filtered list of devices. <p>
     *
     * Applies status and type filters before partitioning the data into pages. Each device's compliance score is
     * dynamically adjusted based on the provided security preferences.
     *
     * @param loggedInEmail     the email of the authenticated user
     * @param pageToken         the current page index string
     * @param size              number of items per page
     * @param filterStatusStr   filter by device status (e.g., APPROVED)
     * @param filterType        filter by OS type (e.g., Android, Windows)
     * @param disabledKeys      set of security check keys to ignore
     * @return q {@link DevicePageResponse} containing the filtered devices
     */
    public DevicePageResponse getDevicesPaged(
            String loggedInEmail, String pageToken, int size,
            String filterStatusStr, String filterType, Set<String> disabledKeys) {

        Set<String> off = disabledKeys == null ? Set.of() : disabledKeys;

        List<DeviceDetail> filteredList = getAllMappedDevices(loggedInEmail);
        DeviceStatus filterStatus = DeviceStatus.fromString(filterStatusStr);

        if (filterStatus != null && filterStatus != DeviceStatus.ALL) {
            filteredList = filteredList.stream()
                    .filter(d -> d.status() != null && d.status().equalsIgnoreCase(filterStatus.getValue()))
                    .toList();
        }

        if (filterType != null && !filterType.equals("all")) {
            filteredList = filteredList.stream()
                    .filter(d -> d.os() != null && d.os().toLowerCase().startsWith(filterType.toLowerCase()))
                    .toList();
        }

        int page = GoogleServiceHelperMethods.getPage(pageToken);
        int totalDevices = filteredList.size();
        int startIndex = (page - 1) * size;
        int endIndex = Math.min(startIndex + size, totalDevices);

        List<DeviceDetail> pagedDevices = (startIndex >= totalDevices)
                ? Collections.emptyList()
                : filteredList.subList(startIndex, endIndex);

        List<DeviceDetail> adjustedPage = pagedDevices.stream()
                .map(d -> deviceComplianceScorer.applyPreferenceAdjustedCompliance(d, off))
                .toList();

        String nextTokenToReturn = (endIndex < totalDevices) ? String.valueOf(page + 1) : null;
        return new DevicePageResponse(adjustedPage, nextTokenToReturn);
    }

    /**
     * Calculates a comprehensive security overview for all devices associated with a user. <p>
     *
     * This includes total counts, compliance percentages, a detailed score breakdown, and security warnings based
     * on active preferences.
     *
     * @param loggedInEmail the email of the authenticated user
     * @param disabledKeys  set of security check keys to ignore
     * @return q {@link DeviceOverviewResponse} with aggregated metrics
     */
    public DeviceOverviewResponse getDevicesPageOverview(String loggedInEmail, Set<String> disabledKeys) {
        Set<String> off = disabledKeys == null ? Set.of() : disabledKeys;
        List<DeviceDetail> allDevices = getAllMappedDevices(loggedInEmail);

        int totalDevices = allDevices.size();
        int approvedDevices = 0;
        int nonCompliantDevices = 0;
        int totalScoreSum = 0;

        int totalLockScreenCount = 0;
        int totalEncryptionCount = 0;
        int totalOsVersionCount = 0;
        int totalIntegrityCount = 0;

        boolean ignLock = SecurityPreferenceScoreSupport.preferenceDisabled(off, "mobile-devices", "lockscreen");
        boolean ignEnc = SecurityPreferenceScoreSupport.preferenceDisabled(off, "mobile-devices", "encryption");
        boolean ignOs = SecurityPreferenceScoreSupport.preferenceDisabled(off, "mobile-devices", "osVersion");
        boolean ignInt = SecurityPreferenceScoreSupport.preferenceDisabled(off, "mobile-devices", "integrity");

        for (DeviceDetail device : allDevices) {
            String status = device.status();
            if ("APPROVED".equalsIgnoreCase(status) || "ACTIVE".equalsIgnoreCase(status) || "PROVISIONED".equalsIgnoreCase(status)) {
                approvedDevices++;
            }

            int adj = deviceComplianceScorer.adjustedDeviceComplianceScore(device, ignLock, ignEnc, ignOs, ignInt);
            totalScoreSum += adj;

            if (!ignLock && !device.lockSecure()) totalLockScreenCount++;
            if (!ignEnc && !device.encSecure()) totalEncryptionCount++;
            if (!ignInt && !device.intSecure()) totalIntegrityCount++;
            if (!ignOs && !device.osSecure()) totalOsVersionCount++;

            if (adj < 75) {
                nonCompliantDevices++;
            }
        }

        if (totalDevices == 0) {
            SectionWarningsDto warnings = SectionWarningEvaluator.with(off)
                    .check("lockScreenWarning", totalLockScreenCount, "mobile-devices", "lockscreen")
                    .check("encryptionWarning", totalEncryptionCount, "mobile-devices", "encryption")
                    .check("osVersionWarning", totalOsVersionCount, "mobile-devices", "osVersion")
                    .check("integrityWarning", totalIntegrityCount, "mobile-devices", "integrity")
                    .build();
            return new DeviceOverviewResponse(
                    0, 0, 0,
                    null,
                    0, 0, 0, 0,
                    null,
                    warnings
            );
        }

        int securityScore = (int) Math.round((double) totalScoreSum / totalDevices);

        SecurityScoreBreakdownDto breakdown = deviceComplianceScorer.buildDevicesBreakdown(
                totalDevices, totalLockScreenCount, totalEncryptionCount, totalOsVersionCount, totalIntegrityCount,
                securityScore, ignLock, ignEnc, ignOs, ignInt);

        SectionWarningsDto warnings = SectionWarningEvaluator.with(off)
                .check("lockScreenWarning", totalLockScreenCount, "mobile-devices", "lockscreen")
                .check("encryptionWarning", totalEncryptionCount, "mobile-devices", "encryption")
                .check("osVersionWarning", totalOsVersionCount, "mobile-devices", "osVersion")
                .check("integrityWarning", totalIntegrityCount, "mobile-devices", "integrity")
                .build();

        return new DeviceOverviewResponse(
                totalDevices, nonCompliantDevices, approvedDevices, securityScore,
                totalLockScreenCount, totalEncryptionCount, totalOsVersionCount, totalIntegrityCount,
                breakdown, warnings
        );
    }

    /**
     * Extracts a unique list of operating system types present in the device fleet.
     *
     * @param loggedInEmail the email of the authenticated user
     */
    public List<String> getUniqueDeviceTypes(String loggedInEmail) {
        List<DeviceDetail> allDevices = getAllMappedDevices(loggedInEmail);
        Set<String> uniqueTypes = new HashSet<>();

        for (DeviceDetail device : allDevices) {
            String os = device.os();
            if (os != null && !os.isBlank() && !os.equalsIgnoreCase("Onbekend")) {
                String baseOs = os.split(" ")[0];
                if (!baseOs.isBlank()) {
                    uniqueTypes.add(baseOs);
                }
            }
        }

        List<String> sortedTypes = new ArrayList<>(uniqueTypes);
        sortedTypes.sort(String.CASE_INSENSITIVE_ORDER);
        return sortedTypes;
    }

    private List<DeviceDetail> getAllMappedDevices(String loggedInEmail) {
        DeviceCacheEntry cachedData = deviceCacheService.getOrFetchData(loggedInEmail);
        List<DeviceDetail> allDevices = new ArrayList<>();

        // Map Mobile
        if (cachedData.mobileDevices() != null) {
            cachedData.mobileDevices().forEach(d -> allDevices.add(mapper.mapMobile(d)));
        }
        // Map ChromeOS
        if (cachedData.chromeOsDevices() != null) {
            cachedData.chromeOsDevices().forEach(d -> allDevices.add(mapper.mapChromeOs(d)));
        }
        // Map Endpoints (Windows/Mac)
        if (cachedData.endpointDevices() != null) {
            cachedData.endpointDevices().forEach(d -> allDevices.add(mapper.mapEndpoint(d)));
        }

        return allDevices;
    }
}