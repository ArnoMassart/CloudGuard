package com.cloudmen.cloudguard.service;

import com.cloudmen.cloudguard.dto.devices.*;
import com.cloudmen.cloudguard.dto.password.SecurityScoreBreakdownDto;
import com.cloudmen.cloudguard.dto.password.SecurityScoreFactorDto;
import com.cloudmen.cloudguard.service.cache.GoogleDeviceCacheService;
import com.cloudmen.cloudguard.utility.DateTimeConverter;
import com.cloudmen.cloudguard.utility.GoogleServiceHelperMethods;
import com.cloudmen.cloudguard.utility.UtilityFunctions;
import com.google.api.services.admin.directory.model.ChromeOsDevice;
import com.google.api.services.admin.directory.model.MobileDevice;
import com.google.api.services.cloudidentity.v1.model.GoogleAppsCloudidentityDevicesV1Device;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.MessageSource;
import org.springframework.context.i18n.LocaleContextHolder;
import org.springframework.stereotype.Service;

import java.util.*;

@Service
public class GoogleDeviceService {

    private static final Logger log = LoggerFactory.getLogger(GoogleDeviceService.class);
    private final GoogleDeviceCacheService deviceCacheService;
    private final MessageSource messageSource;

    public GoogleDeviceService(GoogleDeviceCacheService deviceCacheService, MessageSource messageSource) {
        this.deviceCacheService = deviceCacheService;
        this.messageSource = messageSource;
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
            String filterStatusStr, String filterType) {

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

        String nextTokenToReturn = (endIndex < totalDevices) ? String.valueOf(page + 1) : null;
        return new DevicePageResponse(pagedDevices, nextTokenToReturn);
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
        List<DeviceDetail> allDevices = getAllMappedDevices(loggedInEmail);

        int totalDevices = allDevices.size();
        int approvedDevices = 0;
        int nonCompliantDevices = 0;
        int totalScoreSum = 0;

        int totalLockScreenCount = 0;
        int totalEncryptionCount = 0;
        int totalOsVersionCount = 0;
        int totalIntegrityCount = 0;

        for (DeviceDetail device : allDevices) {
            String status = device.status();
            if ("APPROVED".equalsIgnoreCase(status) || "ACTIVE".equalsIgnoreCase(status) || "PROVISIONED".equalsIgnoreCase(status)) {
                approvedDevices++;
            }

            totalScoreSum += device.complianceScore();

            if (!device.lockSecure()) totalLockScreenCount++;
            if (!device.encSecure()) totalEncryptionCount++;
            if (!device.intSecure()) totalIntegrityCount++;
            if (!device.osSecure()) totalOsVersionCount++;

            if (device.complianceScore() < 75) {
                nonCompliantDevices++;
            }
        }

        int securityScore = totalDevices == 0 ? 100 : (int) Math.round((double) totalScoreSum / totalDevices);

        SecurityScoreBreakdownDto breakdown = buildDevicesBreakdown(
                totalDevices, totalLockScreenCount, totalEncryptionCount, totalOsVersionCount, totalIntegrityCount, securityScore);

        return new DeviceOverviewResponse(
                totalDevices, nonCompliantDevices, approvedDevices, securityScore,
                totalLockScreenCount, totalEncryptionCount, totalOsVersionCount, totalIntegrityCount,
                breakdown
        );
    }

    private SecurityScoreBreakdownDto buildDevicesBreakdown(int totalDevices, int lockScreenCount, int encryptionCount, int osVersionCount, int integrityCount, int securityScore) {
        int lockScore = totalDevices == 0 ? 100 : (int) Math.round((totalDevices - lockScreenCount) * 100.0 / totalDevices);
        int encScore = totalDevices == 0 ? 100 : (int) Math.round((totalDevices - encryptionCount) * 100.0 / totalDevices);
        int osScore = totalDevices == 0 ? 100 : (int) Math.round((totalDevices - osVersionCount) * 100.0 / totalDevices);
        int intScore = totalDevices == 0 ? 100 : (int) Math.round((totalDevices - integrityCount) * 100.0 / totalDevices);

        Locale locale = LocaleContextHolder.getLocale();

        var factors = java.util.List.of(
                new SecurityScoreFactorDto(messageSource.getMessage("devices.score.factor.lock_screen.title", null, locale), lockScreenCount == 0 ? messageSource.getMessage("devices.score.factor.lock_screen.description.all", null, locale) : messageSource.getMessage("devices.score.factor.lock_screen.description", new Object[]{lockScreenCount}, locale), lockScore, 100, severity(lockScore)),
                new SecurityScoreFactorDto(messageSource.getMessage("devices.score.factor.enc.title", null, locale), encryptionCount == 0 ? messageSource.getMessage("devices.score.factor.enc.description.all", null, locale) : messageSource.getMessage("devices.score.factor.enc.description", new Object[]{encryptionCount}, locale), encScore, 100, severity(encScore)),
                new SecurityScoreFactorDto(messageSource.getMessage("devices.score.factor.os_version.title", null, locale), osVersionCount == 0 ? messageSource.getMessage("devices.score.factor.os_version.description.all", null, locale) : messageSource.getMessage("devices.score.factor.os_version.description", new Object[]{osVersionCount}, locale), osScore, 100, severity(osScore)),
                new SecurityScoreFactorDto(messageSource.getMessage("devices.score.factor.int.title", null, locale), integrityCount == 0 ? messageSource.getMessage("devices.score.factor.int.description.none", null, locale) : messageSource.getMessage("devices.score.factor.int.description", new Object[]{integrityCount}, locale), intScore, 100, severity(intScore))
        );
        String status = securityScore == 100 ? "perfect" : securityScore >= 75 ? "good" : securityScore > 50 ? "average" : "bad";
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
        Locale locale = LocaleContextHolder.getLocale();

        String userEmail = (d.getName() != null && !d.getName().isEmpty()) ? d.getName().get(0) : messageSource.getMessage("unknown", null, locale);
        String userName = UtilityFunctions.capitalizeWords(userEmail.split("@")[0].replace(".", " "));
        String deviceName = userName.split(" ")[0] + "'s " + (d.getOs() != null && d.getOs().contains("iOS") ? "iPhone" : "Android");
        String os = d.getOs() != null ? d.getOs() : messageSource.getMessage("unknown", null, locale);
        String syncTime = d.getLastSync() != null ? DateTimeConverter.convertToTimeAgo(d.getLastSync().getValue()) : messageSource.getMessage("never", null, locale);
        String status = d.getStatus() != null ? UtilityFunctions.capitalizeWords(d.getStatus()) : messageSource.getMessage("unknown", null, locale);

        boolean lockSecure = "PASSWORD_SET".equalsIgnoreCase(d.getDevicePasswordStatus());
        String lockText = lockSecure ? messageSource.getMessage("devices.detail.lock_secure", null, locale) : messageSource.getMessage("devices.detail.lock_secure.not", null, locale);

        boolean encSecure = "ENCRYPTED".equalsIgnoreCase(d.getEncryptionStatus()) || "ENCRYPTING".equalsIgnoreCase(d.getEncryptionStatus());
        String encText = encSecure ? messageSource.getMessage("devices.detail.enc_secure", null, locale) : messageSource.getMessage("devices.detail.enc_secure.not", null, locale);

        boolean intSecure = "UNCOMPROMISED".equalsIgnoreCase(d.getDeviceCompromisedStatus());
        String intText = intSecure ? messageSource.getMessage("devices.detail.int_secure", null, locale) : messageSource.getMessage("devices.detail.int_secure.not", null, locale);

        boolean osSecure = checkOsVersion(os);
        String osText = osSecure ? os + " - Up to date" : os + " - " + messageSource.getMessage("devices.detail.old", null, locale);

        int score = calculateScore(lockSecure, encSecure, intSecure, osSecure);

        return new DeviceDetail(
                d.getResourceId(), "MOBILE", userName, userEmail, deviceName,
                d.getModel(), os, syncTime, status, score,
                lockSecure, lockText, encSecure, encText,
                osSecure, osText, intSecure, intText
        );
    }

    private DeviceDetail mapChromeOs(ChromeOsDevice d) {
        Locale locale = LocaleContextHolder.getLocale();

        String userEmail = (d.getRecentUsers() != null && !d.getRecentUsers().isEmpty()) ? d.getRecentUsers().get(0).getEmail() : messageSource.getMessage("unknown", null, locale);
        String userName = UtilityFunctions.capitalizeWords(userEmail.split("@")[0].replace(".", " "));
        String deviceName = "Chromebook (" + (d.getSerialNumber() != null ? d.getSerialNumber() : messageSource.getMessage("unknown", null, locale)) + ")";
        String os = "ChromeOS " + (d.getOsVersion() != null ? d.getOsVersion() : "");
        String syncTime = d.getLastSync() != null ? DateTimeConverter.convertToTimeAgo(d.getLastSync().getValue()) : messageSource.getMessage("never", null, locale);
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
                true, messageSource.getMessage("devices.detail.lock.text", null, locale),
                true, messageSource.getMessage("devices.detail.enc.text", null, locale),
                true, os + " - "+messageSource.getMessage("devices.detail.os.text", null, locale),
                true, messageSource.getMessage("devices.detail.int.text", null, locale)
        );
    }

    private DeviceDetail mapEndpoint(DeviceCacheEntry.EndpointWrapper wrapper) {
        // Haal het apparaat en de gebruikers uit het envelopje
        var d = wrapper.device();
        var users = wrapper.deviceUsers();

        Locale locale = LocaleContextHolder.getLocale();

        String type = d.getDeviceType();
        String userEmail = messageSource.getMessage("unknown", null, locale);
        DeviceStatus status = DeviceStatus.APPROVED;

        // Lees uit de meegeleverde gebruikerslijst
        if (users != null && !users.isEmpty()) {
            var firstUser = users.get(0);
            userEmail = firstUser.getUserEmail() != null ? firstUser.getUserEmail() : messageSource.getMessage("unknown", null, locale);

            if (firstUser.getCompromisedState() != null && "COMPROMISED".equalsIgnoreCase(firstUser.getCompromisedState())) {
                status = DeviceStatus.BLOCKED;
            }
        }

        String userName = UtilityFunctions.capitalizeWords(userEmail.split("@")[0].replace(".", " "));
        String deviceType = "MAC_OS".equalsIgnoreCase(type) ? "MAC" : (type != null ? type : "WINDOWS");
        String deviceName = userName.split(" ")[0] + "'s " + ("MAC".equals(deviceType) ? "MacBook/Mac" : "Windows PC");
        String os = ("MAC".equals(deviceType) ? "MacOS " : "Windows ") + (d.getOsVersion() != null ? d.getOsVersion() : "10.0");
        String model = "Endpoint (GCPW/EV)";

        String syncTime = messageSource.getMessage("never", null, locale);
        if (d.getLastSyncTime() != null) {
            try {
                long millis = java.time.Instant.parse(d.getLastSyncTime()).toEpochMilli();
                syncTime = DateTimeConverter.convertToTimeAgo(millis);
            } catch (Exception ignored) {}
        }

        boolean encSecure = "ENCRYPTED".equalsIgnoreCase(d.getEncryptionState());
        String encText = encSecure ? messageSource.getMessage("devices.endpoint.enc_secure", null, locale) : messageSource.getMessage("devices.endpoint.enc_secure.not", null, locale);

        boolean intSecure = "UNCOMPROMISED".equalsIgnoreCase(d.getCompromisedState());
        String intText = intSecure ? messageSource.getMessage("devices.endpoint.int_secure", null, locale) : messageSource.getMessage("devices.endpoint.int_secure.not", null, locale);

        boolean lockSecure = true;
        String lockText = messageSource.getMessage("devices.endpoint.lock", null, locale);
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