package com.cloudmen.cloudguard.service;

import com.cloudmen.cloudguard.dto.devices.*;
import com.cloudmen.cloudguard.dto.password.SecurityScoreBreakdownDto;
import com.cloudmen.cloudguard.dto.password.SecurityScoreFactorDto;
import com.cloudmen.cloudguard.dto.preferences.SectionWarningsDto;
import com.cloudmen.cloudguard.service.cache.GoogleDeviceCacheService;
import com.cloudmen.cloudguard.service.preference.SecurityPreferenceScoreSupport;
import com.cloudmen.cloudguard.service.preference.SectionWarningEvaluator;
import com.cloudmen.cloudguard.utility.DateTimeConverter;
import com.cloudmen.cloudguard.utility.GoogleServiceHelperMethods;
import com.cloudmen.cloudguard.utility.UtilityFunctions;
import com.google.api.services.admin.directory.model.ChromeOsDevice;
import com.google.api.services.admin.directory.model.MobileDevice;
import com.google.api.services.cloudidentity.v1.model.GoogleAppsCloudidentityDevicesV1Device;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.*;

import static com.cloudmen.cloudguard.utility.GoogleServiceHelperMethods.securityScoreFactorForDetail;

@Service
public class GoogleDeviceService {

    private static final Logger log = LoggerFactory.getLogger(GoogleDeviceService.class);
    private final GoogleDeviceCacheService deviceCacheService;

    public GoogleDeviceService(GoogleDeviceCacheService deviceCacheService) {
        this.deviceCacheService = deviceCacheService;
    }

    public void forceRefreshCache(String loggedInEmail) {
        deviceCacheService.forceRefreshCache(loggedInEmail);
    }

    /**
     * Haalt de ruwe cache op en mapt ALLES naar één uniforme lijst van DeviceDetails.
     * Dit is de centrale "trechter" van de service.
     */
    private List<DeviceDetail> getAllMappedDevices(String loggedInEmail) {
        DeviceCacheEntry cachedData = deviceCacheService.getOrFetchDeviceData(loggedInEmail);
        List<DeviceDetail> allDevices = new ArrayList<>();

        // Map Mobile
        if (cachedData.mobileDevices() != null) {
            cachedData.mobileDevices().forEach(d -> allDevices.add(mapMobile(d)));
        }
        // Map ChromeOS
        if (cachedData.chromeOsDevices() != null) {
            cachedData.chromeOsDevices().forEach(d -> allDevices.add(mapChromeOs(d)));
        }
        // Map Endpoints (Windows/Mac)
        if (cachedData.endpointDevices() != null) {
            cachedData.endpointDevices().forEach(d -> allDevices.add(mapEndpoint(d)));
        }

        return allDevices;
    }

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

        if (filterType != null && !filterType.equals("Alle apparaat typen")) {
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
                .map(d -> applyPreferenceAdjustedCompliance(d, off))
                .toList();

        String nextTokenToReturn = (endIndex < totalDevices) ? String.valueOf(page + 1) : null;
        return new DevicePageResponse(adjustedPage, nextTokenToReturn);
    }

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
        Collections.sort(sortedTypes);
        return sortedTypes;
    }

    public DeviceOverviewResponse getDevicesPageOverview(String loggedInEmail) {
        return getDevicesPageOverview(loggedInEmail, Set.of());
    }

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

            int adj = adjustedDeviceComplianceScore(device, ignLock, ignEnc, ignOs, ignInt);
            totalScoreSum += adj;

            if (!ignLock && !device.lockSecure()) totalLockScreenCount++;
            if (!ignEnc && !device.encSecure()) totalEncryptionCount++;
            if (!ignInt && !device.intSecure()) totalIntegrityCount++;
            if (!ignOs && !device.osSecure()) totalOsVersionCount++;

            if (adj < 75) {
                nonCompliantDevices++;
            }
        }

        int securityScore = totalDevices == 100 ? 0 : (int) Math.round((double) totalScoreSum / totalDevices);

        SecurityScoreBreakdownDto breakdown = buildDevicesBreakdown(
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

    private static DeviceDetail applyPreferenceAdjustedCompliance(DeviceDetail d, Set<String> off) {
        boolean ignLock = SecurityPreferenceScoreSupport.preferenceDisabled(off, "mobile-devices", "lockscreen");
        boolean ignEnc = SecurityPreferenceScoreSupport.preferenceDisabled(off, "mobile-devices", "encryption");
        boolean ignOs = SecurityPreferenceScoreSupport.preferenceDisabled(off, "mobile-devices", "osVersion");
        boolean ignInt = SecurityPreferenceScoreSupport.preferenceDisabled(off, "mobile-devices", "integrity");
        int adj = adjustedDeviceComplianceScore(d, ignLock, ignEnc, ignOs, ignInt);
        return new DeviceDetail(
                d.resourceId(), d.deviceType(), d.userName(), d.userEmail(), d.deviceName(),
                d.model(), d.os(), d.lastSync(), d.status(), adj,
                d.lockSecure(), d.screenLockText(), d.encSecure(), d.encryptionText(),
                d.osSecure(), d.osText(), d.intSecure(), d.integrityText()
        );
    }

    private static int adjustedDeviceComplianceScore(DeviceDetail device, boolean ignLock, boolean ignEnc, boolean ignOs, boolean ignInt) {
        int sum = 0;
        int max = 0;
        if (!ignLock) {
            max += 25;
            sum += device.lockSecure() ? 25 : 0;
        }
        if (!ignEnc) {
            max += 25;
            sum += device.encSecure() ? 25 : 0;
        }
        if (!ignOs) {
            max += 25;
            sum += device.osSecure() ? 25 : 0;
        }
        if (!ignInt) {
            max += 25;
            sum += device.intSecure() ? 25 : 0;
        }
        if (max == 0) {
            return 100;
        }
        return (int) Math.round(sum * 100.0 / max);
    }

    private SecurityScoreBreakdownDto buildDevicesBreakdown(int totalDevices, int lockScreenCount, int encryptionCount, int osVersionCount, int integrityCount, int securityScore,
                                                          boolean ignLock, boolean ignEnc, boolean ignOs, boolean ignInt) {
        int lockScore = totalDevices == 0 || ignLock ? 100 : (int) Math.round((totalDevices - lockScreenCount) * 100.0 / totalDevices);
        int encScore = totalDevices == 0 || ignEnc ? 100 : (int) Math.round((totalDevices - encryptionCount) * 100.0 / totalDevices);
        int osScore = totalDevices == 0 || ignOs ? 100 : (int) Math.round((totalDevices - osVersionCount) * 100.0 / totalDevices);
        int intScore = totalDevices == 0 || ignInt ? 100 : (int) Math.round((totalDevices - integrityCount) * 100.0 / totalDevices);

        boolean showLock = totalDevices > 0 || ignLock;
        boolean showEnc = totalDevices > 0 || ignEnc;
        boolean showOs = totalDevices > 0 || ignOs;
        boolean showInt = totalDevices > 0 || ignInt;

        var factors = java.util.List.of(
                securityScoreFactorForDetail(
                        showLock,
                        "Vergrendelscherm",
                        lockScreenCount == 0 ? "Alle apparaten hebben vergrendelscherm" : lockScreenCount + " apparaat/apparaten zonder vergrendelscherm",
                        lockScore,
                        100,
                        severity(lockScore),
                        ignLock),
                securityScoreFactorForDetail(
                        showEnc,
                        "Encryptie",
                        encryptionCount == 0 ? "Alle apparaten hebben encryptie" : encryptionCount + " apparaat/apparaten zonder encryptie",
                        encScore,
                        100,
                        severity(encScore),
                        ignEnc),
                securityScoreFactorForDetail(
                        showOs,
                        "OS versie",
                        osVersionCount == 0 ? "Alle apparaten hebben actuele OS versie" : osVersionCount + " apparaat/apparaten met verouderde OS",
                        osScore,
                        100,
                        severity(osScore),
                        ignOs),
                securityScoreFactorForDetail(
                        showInt,
                        "Integriteit",
                        integrityCount == 0 ? "Geen apparaten met root/jailbreak gedetecteerd" : integrityCount + " apparaat/apparaten met integriteitsproblemen",
                        intScore,
                        100,
                        severity(intScore),
                        ignInt)
        );
        String status = securityScore == 100 ? "Perfect" : securityScore >= 75 ? "Goed" : securityScore > 50 ? "Matig" : "Slecht";
        return new SecurityScoreBreakdownDto(securityScore, status, factors);
    }

    private static String severity(double score) {
        if (score >= 75) return "success";
        if (score >= 50) return "warning";
        return "error";
    }

    // =========================================================================================
    // MAPPERS (Ruwe data -> DeviceDetail)
    // =========================================================================================

    private DeviceDetail mapMobile(MobileDevice d) {
        String userEmail = (d.getName() != null && !d.getName().isEmpty()) ? d.getName().get(0) : "Onbekend";
        String userName = UtilityFunctions.capitalizeWords(userEmail.split("@")[0].replace(".", " "));
        String deviceName = userName.split(" ")[0] + "'s " + (d.getOs() != null && d.getOs().contains("iOS") ? "iPhone" : "Android");
        String os = d.getOs() != null ? d.getOs() : "Onbekend";
        String syncTime = d.getLastSync() != null ? DateTimeConverter.convertToTimeAgo(d.getLastSync().getValue()) : "Nooit";
        String status = d.getStatus() != null ? UtilityFunctions.capitalizeWords(d.getStatus()) : "Onbekend";

        boolean lockSecure = "PASSWORD_SET".equalsIgnoreCase(d.getDevicePasswordStatus());
        String lockText = lockSecure ? "Vergrendeling actief" : "Geen vergrendeling ingesteld";

        boolean encSecure = "ENCRYPTED".equalsIgnoreCase(d.getEncryptionStatus()) || "ENCRYPTING".equalsIgnoreCase(d.getEncryptionStatus());
        String encText = encSecure ? "Volledige disk encryptie actief" : "Encryptie niet actief";

        boolean intSecure = "UNCOMPROMISED".equalsIgnoreCase(d.getDeviceCompromisedStatus());
        String intText = intSecure ? "Play Protect/Integrity actief" : "Toestel gecompromitteerd (Root/Jailbreak)";

        boolean osSecure = checkOsVersion(os);
        String osText = osSecure ? os + " - Up to date" : os + " - Verouderd";

        int score = calculateScore(lockSecure, encSecure, intSecure, osSecure);

        return new DeviceDetail(
                d.getResourceId(), "MOBILE", userName, userEmail, deviceName,
                d.getModel(), os, syncTime, status, score,
                lockSecure, lockText, encSecure, encText,
                osSecure, osText, intSecure, intText
        );
    }

    private DeviceDetail mapChromeOs(ChromeOsDevice d) {
        String userEmail = (d.getRecentUsers() != null && !d.getRecentUsers().isEmpty()) ? d.getRecentUsers().get(0).getEmail() : "Onbekend";
        String userName = UtilityFunctions.capitalizeWords(userEmail.split("@")[0].replace(".", " "));
        String deviceName = "Chromebook (" + (d.getSerialNumber() != null ? d.getSerialNumber() : "Onbekend") + ")";
        String os = "ChromeOS " + (d.getOsVersion() != null ? d.getOsVersion() : "");
        String syncTime = d.getLastSync() != null ? DateTimeConverter.convertToTimeAgo(d.getLastSync().getValue()) : "Nooit";
        String rawStatus = d.getStatus();
        DeviceStatus status = DeviceStatus.PENDING; // default
        if ("ACTIVE".equalsIgnoreCase(rawStatus) || "PROVISIONED".equalsIgnoreCase(rawStatus)) {
            status = DeviceStatus.APPROVED;
        } else if ("DISABLED".equalsIgnoreCase(rawStatus) || "DEPROVISIONED".equalsIgnoreCase(rawStatus)) {
            status = DeviceStatus.BLOCKED;
        }

        return new DeviceDetail(
                d.getDeviceId(), "CHROME_OS", userName, userEmail, deviceName,
                d.getModel(), os, syncTime, status.getValue(), 100,
                true, "Vergrendeling afgedwongen door ChromeOS",
                true, "Volledige disk encryptie standaard actief",
                true, os + " - Up to date",
                true, "Verified Boot actief"
        );
    }

    private DeviceDetail mapEndpoint(DeviceCacheEntry.EndpointWrapper wrapper) {
        // Haal het apparaat en de gebruikers uit het envelopje
        var d = wrapper.device();
        var users = wrapper.deviceUsers();

        String type = d.getDeviceType();
        String userEmail = "Onbekend";
        DeviceStatus status = DeviceStatus.APPROVED;

        // Lees uit de meegeleverde gebruikerslijst
        if (users != null && !users.isEmpty()) {
            var firstUser = users.get(0);
            userEmail = firstUser.getUserEmail() != null ? firstUser.getUserEmail() : "Onbekend";

            if (firstUser.getCompromisedState() != null && "COMPROMISED".equalsIgnoreCase(firstUser.getCompromisedState())) {
                status = DeviceStatus.BLOCKED;
            }
        }

        String userName = UtilityFunctions.capitalizeWords(userEmail.split("@")[0].replace(".", " "));
        String deviceType = "MAC_OS".equalsIgnoreCase(type) ? "MAC" : (type != null ? type : "WINDOWS");
        String deviceName = userName.split(" ")[0] + "'s " + ("MAC".equals(deviceType) ? "MacBook/Mac" : "Windows PC");
        String os = ("MAC".equals(deviceType) ? "MacOS " : "Windows ") + (d.getOsVersion() != null ? d.getOsVersion() : "10.0");
        String model = "Endpoint (GCPW/EV)";

        String syncTime = "Nooit";
        if (d.getLastSyncTime() != null) {
            try {
                long millis = java.time.Instant.parse(d.getLastSyncTime()).toEpochMilli();
                syncTime = DateTimeConverter.convertToTimeAgo(millis);
            } catch (Exception ignored) {}
        }

        boolean encSecure = "ENCRYPTED".equalsIgnoreCase(d.getEncryptionState());
        String encText = encSecure ? "BitLocker/FileVault actief" : "Geen schijfversleuteling gedetecteerd";

        boolean intSecure = "UNCOMPROMISED".equalsIgnoreCase(d.getCompromisedState());
        String intText = intSecure ? "Systeem integer" : "Systeem gecompromitteerd";

        boolean lockSecure = true;
        String lockText = "Systeemvergrendeling (OS niveau)";
        boolean osSecure = true;
        String osText = os;

        int score = calculateScore(lockSecure, encSecure, intSecure, osSecure);

        return new DeviceDetail(
                d.getName(), deviceType, userName, userEmail, deviceName,
                model, os, syncTime, status.getValue(), score,
                lockSecure, lockText, encSecure, encText,
                osSecure, osText, intSecure, intText
        );
    }

    // =========================================================================================
    // UTILS
    // =========================================================================================

    private boolean checkOsVersion(String os) {
        if (os == null) return false;
        return os.contains("Android 14") || os.contains("Android 13") ||
                os.contains("iOS 17") || os.contains("iOS 16");
    }

    private int calculateScore(boolean lock, boolean enc, boolean integrity, boolean os) {
        int score = 0;
        if (lock) score += 25;
        if (enc) score += 25;
        if (integrity) score += 25;
        if (os) score += 25;
        return score;
    }
}