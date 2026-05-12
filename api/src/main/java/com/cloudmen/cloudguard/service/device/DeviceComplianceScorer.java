package com.cloudmen.cloudguard.service.device;

import com.cloudmen.cloudguard.dto.devices.DeviceDetail;
import com.cloudmen.cloudguard.dto.password.SecurityScoreBreakdownDto;
import com.cloudmen.cloudguard.service.preference.SecurityPreferenceScoreSupport;
import org.springframework.stereotype.Component;

import java.util.Set;

import static com.cloudmen.cloudguard.utility.GoogleServiceHelperMethods.securityScoreFactorForDetail;
import static com.cloudmen.cloudguard.utility.GoogleServiceHelperMethods.severity;

@Component
public class DeviceComplianceScorer {
    private int calculateDeviceScore(int totalDevices, int violationCount) {
        if (totalDevices == 0) return 100;

        return (int) Math.round((totalDevices - violationCount) * 100.0 / totalDevices);
    }

    public SecurityScoreBreakdownDto buildDevicesBreakdown(int totalDevices, int lockScreenCount, int encryptionCount, int osVersionCount, int integrityCount, int securityScore) {
        int lockScore = calculateDeviceScore(totalDevices, lockScreenCount);
        int encScore = calculateDeviceScore(totalDevices, encryptionCount);
        int osScore = calculateDeviceScore(totalDevices, osVersionCount);
        int intScore = calculateDeviceScore(totalDevices, integrityCount);

        boolean show = totalDevices > 0;

        var factors = java.util.List.of(
                securityScoreFactorForDetail(
                        show,
                        "Vergrendelscherm",
                        lockScreenCount == 0 ? "Alle apparaten hebben vergrendelscherm" : lockScreenCount + " apparaat/apparaten zonder vergrendelscherm",
                        lockScore,
                        100,
                        severity(lockScore)),
                securityScoreFactorForDetail(
                        show,
                        "Encryptie",
                        encryptionCount == 0 ? "Alle apparaten hebben encryptie" : encryptionCount + " apparaat/apparaten zonder encryptie",
                        encScore,
                        100,
                        severity(encScore)),
                securityScoreFactorForDetail(
                        show,
                        "OS versie",
                        osVersionCount == 0 ? "Alle apparaten hebben actuele OS versie" : osVersionCount + " apparaat/apparaten met verouderde OS",
                        osScore,
                        100,
                        severity(osScore)),
                securityScoreFactorForDetail(
                        show,
                        "Integriteit",
                        integrityCount == 0 ? "Geen apparaten met root/jailbreak gedetecteerd" : integrityCount + " apparaat/apparaten met integriteitsproblemen",
                        intScore,
                        100,
                        severity(intScore))
        );
        String status = securityScore == 100 ? "Perfect" : securityScore >= 75 ? "Goed" : securityScore > 50 ? "Matig" : "Slecht";
        return new SecurityScoreBreakdownDto(securityScore, status, factors);
    }

    public int calculateScore(boolean lock, boolean enc, boolean integrity, boolean os) {
        int score = 0;
        if (lock) score += 25;
        if (enc) score += 25;
        if (integrity) score += 25;
        if (os) score += 25;
        return score;
    }

    public int calculateScore(DeviceDetail device) {
        int sum = 0;
        // Elke check is 25 punten waard, maximaal 100.
        if (device.lockSecure()) sum += 25;
        if (device.encSecure()) sum += 25;
        if (device.osSecure()) sum += 25;
        if (device.intSecure()) sum += 25;
        return sum;
    }
}
