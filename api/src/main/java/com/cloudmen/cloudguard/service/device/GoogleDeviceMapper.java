package com.cloudmen.cloudguard.service.device;

import com.cloudmen.cloudguard.dto.devices.DeviceCacheEntry;
import com.cloudmen.cloudguard.dto.devices.DeviceDetail;
import com.cloudmen.cloudguard.dto.devices.DeviceStatus;
import com.cloudmen.cloudguard.utility.DateTimeConverter;
import com.cloudmen.cloudguard.utility.UtilityFunctions;
import com.google.api.services.admin.directory.model.ChromeOsDevice;
import com.google.api.services.admin.directory.model.MobileDevice;
import com.google.api.services.cloudidentity.v1.model.GoogleAppsCloudidentityDevicesV1Device;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.MessageSource;
import org.springframework.stereotype.Component;

import java.util.Locale;

import static com.cloudmen.cloudguard.utility.GoogleServiceHelperMethods.getMessage;

@Component
public class GoogleDeviceMapper {
    private final DeviceComplianceScorer deviceComplianceScorer;
    private final MessageSource messageSource;

    public GoogleDeviceMapper(DeviceComplianceScorer deviceComplianceScorer, @Qualifier("messageSource") MessageSource messageSource) {
        this.deviceComplianceScorer = deviceComplianceScorer;
        this.messageSource = messageSource;
    }

    public DeviceDetail mapMobile(MobileDevice d, Locale locale) {
        String userEmail = (d.getName() != null && !d.getName().isEmpty()) ? d.getName().get(0) : getMessage(messageSource, "unknown", locale);
        String userName = UtilityFunctions.capitalizeWords(userEmail.split("@")[0].replace(".", " "));
        String deviceName = userName.split(" ")[0] + "'s " + (d.getOs() != null && d.getOs().contains("iOS") ? "iPhone" : "Android");
        String os = d.getOs() != null ? d.getOs() : getMessage(messageSource, "unknown", locale);
        String syncTime = d.getLastSync() != null ? DateTimeConverter.convertToTimeAgo(d.getLastSync().getValue()) : getMessage(messageSource, "never", locale);
        String status = d.getStatus() != null ? UtilityFunctions.capitalizeWords(d.getStatus()) : getMessage(messageSource, "never", locale);

        boolean lockSecure = "PASSWORD_SET".equalsIgnoreCase(d.getDevicePasswordStatus());
        String lockText = lockSecure ? getMessage(messageSource, "devices.detail.lock_secure", locale) : getMessage(messageSource, "devices.detail.lock_secure.not", locale);

        boolean encSecure = "ENCRYPTED".equalsIgnoreCase(d.getEncryptionStatus()) || "ENCRYPTING".equalsIgnoreCase(d.getEncryptionStatus());
        String encText = encSecure ? getMessage(messageSource, "devices.detail.enc_secure", locale) : getMessage(messageSource, "devices.detail.enc_secure.not", locale);

        boolean intSecure = "UNCOMPROMISED".equalsIgnoreCase(d.getDeviceCompromisedStatus());
        String intText = intSecure ? getMessage(messageSource, "devices.detail.int_secure", locale) : getMessage(messageSource, "devices.detail.int_secure.not", locale);

        boolean osSecure = checkOsVersion(os);
        String osText = osSecure ? os + " - "+ getMessage(messageSource, "devices.detail.os.text", locale) : os + " - " + getMessage(messageSource, "devices.detail.old", locale);

        int score = deviceComplianceScorer.calculateScore(lockSecure, encSecure, intSecure, osSecure);

        return new DeviceDetail(
                d.getResourceId(), "MOBILE", userName, userEmail, deviceName,
                d.getModel(), os, syncTime, status, score,
                lockSecure, lockText, encSecure, encText,
                osSecure, osText, intSecure, intText
        );
    }

    public DeviceDetail mapChromeOs(ChromeOsDevice d, Locale locale) {
        String userEmail = (d.getRecentUsers() != null && !d.getRecentUsers().isEmpty()) ? d.getRecentUsers().get(0).getEmail() : getMessage(messageSource, "unknown", locale);
        String userName = UtilityFunctions.capitalizeWords(userEmail.split("@")[0].replace(".", " "));
        String deviceName = "Chromebook (" + (d.getSerialNumber() != null ? d.getSerialNumber() : getMessage(messageSource, "unknown", locale)) + ")";
        String os = "ChromeOS " + (d.getOsVersion() != null ? d.getOsVersion() : "");
        String syncTime = d.getLastSync() != null ? DateTimeConverter.convertToTimeAgo(d.getLastSync().getValue()) : getMessage(messageSource, "never", locale);
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
                true, getMessage(messageSource, "devices.detail.lock.text", locale),
                true, getMessage(messageSource, "devices.detail.enc.text", locale),
                true, os + " - "+ getMessage(messageSource, "devices.detail.os.text", locale),
                true, getMessage(messageSource, "devices.detail.int.text", locale)
        );
    }

    public DeviceDetail mapEndpoint(DeviceCacheEntry.EndpointWrapper wrapper, Locale locale) {
        // Haal het apparaat en de gebruikers uit het envelopje
        var d = wrapper.device();
        var users = wrapper.deviceUsers();

        String type = d.getDeviceType();
        String userEmail = getMessage(messageSource, "unknown", locale);
        DeviceStatus status = DeviceStatus.PENDING;

        // Lees uit de meegeleverde gebruikerslijst
        if (users != null && !users.isEmpty()) {
            var firstUser = users.get(0);
            userEmail = firstUser.getUserEmail() != null ? firstUser.getUserEmail() : getMessage(messageSource, "unknown", locale);

            String managementState = firstUser.getManagementState();

            if (managementState != null) {
                status = switch (managementState.toUpperCase()) {
                    case "BLOCKED", "WIPING", "WIPED" -> DeviceStatus.BLOCKED;
                    case "APPROVED" -> DeviceStatus.APPROVED;
                    default -> DeviceStatus.PENDING;
                };
            }
        }

        String userName = UtilityFunctions.capitalizeWords(userEmail.split("@")[0].replace(".", " "));
        String deviceType = "MAC_OS".equalsIgnoreCase(type) ? "MAC" : (type != null ? type : "WINDOWS");
        String deviceName = userName.split(" ")[0] + "'s " + ("MAC".equals(deviceType) ? "MacBook/Mac" : "Windows PC");
        String os = getOs(d, deviceType);
        String model = "Endpoint (GCPW/EV)";

        String syncTime = getMessage(messageSource, "never", locale);
        if (d.getLastSyncTime() != null) {
            try {
                long millis = java.time.Instant.parse(d.getLastSyncTime()).toEpochMilli();
                syncTime = DateTimeConverter.convertToTimeAgo(millis);
            } catch (Exception ignored) {}
        }

        boolean encSecure = "ENCRYPTED".equalsIgnoreCase(d.getEncryptionState());
        String encText = encSecure ? getMessage(messageSource, "devices.endpoint.enc_secure", locale) : getMessage(messageSource, "devices.endpoint.enc_secure.not", locale);

        boolean intSecure = "UNCOMPROMISED".equalsIgnoreCase(d.getCompromisedState());
        String intText = intSecure ? getMessage(messageSource, "devices.endpoint.int_secure", locale) : getMessage(messageSource, "devices.endpoint.int_secure.not", locale);

        boolean lockSecure = true;
        String lockText = getMessage(messageSource, "devices.endpoint.lock", locale);
        boolean osSecure = true;

        int score = deviceComplianceScorer.calculateScore(lockSecure, encSecure, intSecure, osSecure);

        return new DeviceDetail(
                d.getName(), deviceType, userName, userEmail, deviceName,
                model, os, syncTime, status.getValue(), score,
                lockSecure, lockText, encSecure, encText,
                osSecure, os, intSecure, intText
        );
    }

    private static String getOs(GoogleAppsCloudidentityDevicesV1Device d, String deviceType) {
        String rawOsVersion = d.getOsVersion() != null ? d.getOsVersion().trim() : "10.0";
        String os;

        if ("MAC".equals(deviceType)) {
            // Als "Mac" al in de string zit (bijv. "Mac OS X 14.4"), gebruik die dan direct
            os = rawOsVersion.toLowerCase().contains("mac") ? rawOsVersion : "MacOS " + rawOsVersion;
        } else {
            // Als "Windows" al in de string zit (bijv. "Windows 10.0..."), gebruik die direct
            os = rawOsVersion.toLowerCase().contains("windows") ? rawOsVersion : "Windows " + rawOsVersion;
        }
        return os;
    }

    private boolean checkOsVersion(String os) {
        if (os == null) return false;
        return os.contains("Android 14") || os.contains("Android 13") ||
                os.contains("iOS 17") || os.contains("iOS 16");
    }
}
