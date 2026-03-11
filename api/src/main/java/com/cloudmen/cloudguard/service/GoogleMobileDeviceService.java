package com.cloudmen.cloudguard.service;

import com.cloudmen.cloudguard.dto.devices.*;
import com.cloudmen.cloudguard.service.cache.GoogleMobileDeviceCacheService;
import com.cloudmen.cloudguard.utility.DateTimeConverter;
import com.cloudmen.cloudguard.utility.GoogleServiceHelperMethods;
import com.cloudmen.cloudguard.utility.UtilityFunctions;
import com.google.api.services.admin.directory.model.MobileDevice;
import org.springframework.stereotype.Service;

import java.util.*;

@Service
public class GoogleMobileDeviceService {
    private final GoogleMobileDeviceCacheService mobileDeviceCacheService;

    public GoogleMobileDeviceService(GoogleMobileDeviceCacheService mobileDeviceCacheService) {
        this.mobileDeviceCacheService = mobileDeviceCacheService;
    }

    public void forceRefreshCache(String loggedInEmail) {
        mobileDeviceCacheService.forceRefreshCache(loggedInEmail);
    }

    public MobileDevicePageResponse getMobileDevicesPaged(
            String loggedInEmail, String pageToken, int size,
            String filterStatusStr, String filterType, boolean isTestMode) {

        MobileDeviceCacheEntry cachedData = mobileDeviceCacheService.getOrFetchDeviceData(loggedInEmail, isTestMode);
        List<MobileDevice> filteredList = cachedData.allDevices();
        MobileDeviceStatus filterStatus = MobileDeviceStatus.fromString(filterStatusStr);

        if (filterStatus != null && filterStatus != MobileDeviceStatus.ALL) {
            filteredList = filteredList.stream()
                    .filter(d -> d.getStatus() != null && d.getStatus().equalsIgnoreCase(filterStatus.getValue()))
                    .toList();
        }
        if (filterType != null && !filterType.equals("Alle apparaat typen")) {
            filteredList = filteredList.stream()
                    .filter(d -> d.getOs() != null && d.getOs().toLowerCase().startsWith(filterType.toLowerCase()))
                    .toList();
        }

        int page = GoogleServiceHelperMethods.getPage(pageToken);

        int totalDevices = filteredList.size();
        int startIndex = (page - 1) * size;
        int endIndex = Math.min(startIndex + size, totalDevices);

        List<MobileDevice> pagedDevices = (startIndex >= totalDevices)
                ? Collections.emptyList()
                : filteredList.subList(startIndex, endIndex);

        List<MobileDeviceDetail> mappedDevices = pagedDevices.stream().map(this::mapToDetail).toList();

        String nextTokenToReturn = (endIndex < totalDevices) ? String.valueOf(page + 1) : null;
        return new MobileDevicePageResponse(mappedDevices, nextTokenToReturn);
    }

    public List<String> getUniqueDeviceTypes(String loggedInEmail, boolean isTestMode) {
        MobileDeviceCacheEntry cachedData = mobileDeviceCacheService.getOrFetchDeviceData(loggedInEmail, isTestMode);
        Set<String> uniqueTypes = new HashSet<>();

        for (MobileDevice device : cachedData.allDevices()) {
            if (device.getOs() != null && !device.getOs().trim().isEmpty()) {
                String baseOs = device.getOs().split(" ")[0];
                uniqueTypes.add(baseOs);
            }
        }

        List<String> sortedTypes = new ArrayList<>(uniqueTypes);
        Collections.sort(sortedTypes);
        return sortedTypes;
    }

    public MobileDeviceOverviewResponse getMobileDevicesPageOverview(String loggedInEmail, boolean isTestMode) {
        MobileDeviceCacheEntry cachedData = mobileDeviceCacheService.getOrFetchDeviceData(loggedInEmail, isTestMode);
        List<MobileDevice> allDevices = cachedData.allDevices();

        int totalDevices = 0;
        int approvedDevices = 0;
        int nonCompliantDevices = 0;
        int totalScoreSum = 0;
        int totalLockScreenCount = 0;
        int totalEncryptionCount = 0;
        int totalOsVersionCount = 0;
        int totalIntegrityCount = 0;

        for (MobileDevice device : allDevices) {
            totalDevices++;

            if ("APPROVED".equalsIgnoreCase(device.getStatus())) {
                approvedDevices++;
            }

            boolean lockSecure = "PASSWORD_SET".equalsIgnoreCase(device.getDevicePasswordStatus());
            boolean encSecure = "ENCRYPTED".equalsIgnoreCase(device.getEncryptionStatus()) ||
                    "ENCRYPTING".equalsIgnoreCase(device.getEncryptionStatus());
            boolean intSecure = "UNCOMPROMISED".equalsIgnoreCase(device.getDeviceCompromisedStatus());
            boolean osSecure = checkOsVersion(device.getOs());

            int deviceScore = 0;

            if (lockSecure) deviceScore += 25; else totalLockScreenCount++;
            if (encSecure) deviceScore += 25; else totalEncryptionCount++;
            if (intSecure) deviceScore += 25; else totalIntegrityCount++;
            if (osSecure) deviceScore += 25; else totalOsVersionCount++;

            totalScoreSum += deviceScore;

            if (deviceScore < 75) nonCompliantDevices++;
        }

        int securityScore = totalDevices == 0 ? 0 : (int) Math.round((double) totalScoreSum / totalDevices);

        return new MobileDeviceOverviewResponse(
                totalDevices, nonCompliantDevices, approvedDevices, securityScore,
                totalLockScreenCount, totalEncryptionCount, totalOsVersionCount, totalIntegrityCount
        );
    }

    private MobileDeviceDetail mapToDetail(MobileDevice device) {
        String email = (device.getName() != null && !device.getName().isEmpty()) ? device.getName().get(0) : "Onbekend";
        String userName = email.split("@")[0].replace(".", " ");
        String deviceName = userName.split(" ")[0].toLowerCase() + "'s " + (device.getOs() != null && device.getOs().contains("iOS") ? "iPhone" : "Android");

        String pwdStatus = device.getDevicePasswordStatus();
        boolean lockSecure = "PASSWORD_SET".equalsIgnoreCase(pwdStatus);
        String lockText = lockSecure ? "PIN code/Wachtwoord actief" : "Geen vergrendeling ingesteld";

        String encStatus = device.getEncryptionStatus();
        boolean encSecure = "ENCRYPTED".equalsIgnoreCase(encStatus) || "ENCRYPTING".equalsIgnoreCase(encStatus);
        String encText = encSecure ? "Volledige disk encryptie actief" : "Encryptie niet actief";

        String compStatus = device.getDeviceCompromisedStatus();
        boolean intSecure = "UNCOMPROMISED".equalsIgnoreCase(compStatus);
        String intText = intSecure ? "Play Protect actief, geen root detectie" : "Toestel gecompromitteerd (Root/Jailbreak)";

        String os = device.getOs() != null ? device.getOs() : "Onbekend";
        boolean osSecure = checkOsVersion(os);
        String osText = osSecure ? os + " - Up to date" : os + " - Verouderd";

        int compliance = 0;
        if (lockSecure) compliance += 25;
        if (encSecure) compliance += 25;
        if (intSecure) compliance += 25;
        if (osSecure) compliance += 25;

        String syncTime = device.getLastSync() != null ? DateTimeConverter.convertToTimeAgo(device.getLastSync()) : "Nooit";

        return new MobileDeviceDetail(
                device.getResourceId(),
                UtilityFunctions.capitalizeWords(userName),
                email,
                deviceName,
                device.getModel(),
                os,
                syncTime,
                UtilityFunctions.capitalizeWords(device.getStatus()),
                compliance,
                lockSecure, lockText,
                encSecure, encText,
                osSecure, osText,
                intSecure, intText
        );
    }

    private boolean checkOsVersion(String os) {
        if (os == null) return false;
        return os.contains("Android 14") || os.contains("Android 13") ||
                os.contains("iOS 17") || os.contains("iOS 16");
    }
}
