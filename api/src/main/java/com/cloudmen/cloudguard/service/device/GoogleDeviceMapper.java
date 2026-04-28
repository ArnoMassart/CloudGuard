package com.cloudmen.cloudguard.service.device;

import com.cloudmen.cloudguard.dto.devices.DeviceCacheEntry;
import com.cloudmen.cloudguard.dto.devices.DeviceDetail;
import com.cloudmen.cloudguard.dto.devices.DeviceStatus;
import com.cloudmen.cloudguard.utility.DateTimeConverter;
import com.cloudmen.cloudguard.utility.UtilityFunctions;
import com.google.api.services.admin.directory.model.ChromeOsDevice;
import com.google.api.services.admin.directory.model.MobileDevice;
import org.springframework.stereotype.Component;

@Component
public class GoogleDeviceMapper {
    private final DeviceComplianceScorer deviceComplianceScorer;

    public GoogleDeviceMapper(DeviceComplianceScorer deviceComplianceScorer) {
        this.deviceComplianceScorer = deviceComplianceScorer;
    }

    public DeviceDetail mapMobile(MobileDevice d) {
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

        int score = deviceComplianceScorer.calculateScore(lockSecure, encSecure, intSecure, osSecure);

        return new DeviceDetail(
                d.getResourceId(), "MOBILE", userName, userEmail, deviceName,
                d.getModel(), os, syncTime, status, score,
                lockSecure, lockText, encSecure, encText,
                osSecure, osText, intSecure, intText
        );
    }

    public DeviceDetail mapChromeOs(ChromeOsDevice d) {
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

    public DeviceDetail mapEndpoint(DeviceCacheEntry.EndpointWrapper wrapper) {
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

        int score = deviceComplianceScorer.calculateScore(lockSecure, encSecure, intSecure, osSecure);

        return new DeviceDetail(
                d.getName(), deviceType, userName, userEmail, deviceName,
                model, os, syncTime, status.getValue(), score,
                lockSecure, lockText, encSecure, encText,
                osSecure, osText, intSecure, intText
        );
    }

    private boolean checkOsVersion(String os) {
        if (os == null) return false;
        return os.contains("Android 14") || os.contains("Android 13") ||
                os.contains("iOS 17") || os.contains("iOS 16");
    }
}
