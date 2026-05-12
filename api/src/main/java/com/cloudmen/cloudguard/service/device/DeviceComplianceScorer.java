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
    public DeviceDetail applyPreferenceAdjustedCompliance(DeviceDetail d, Set<String> off) {
        boolean ignLock = SecurityPreferenceScoreSupport.preferenceDisabled(off, "devices", "lockscreen");
        boolean ignEnc = SecurityPreferenceScoreSupport.preferenceDisabled(off, "devices", "encryption");
        boolean ignOs = SecurityPreferenceScoreSupport.preferenceDisabled(off, "devices", "osVersion");
        boolean ignInt = SecurityPreferenceScoreSupport.preferenceDisabled(off, "devices", "integrity");
        int adj = adjustedDeviceComplianceScore(d, ignLock, ignEnc, ignOs, ignInt);
        return new DeviceDetail(
                d.resourceId(), d.deviceType(), d.userName(), d.userEmail(), d.deviceName(),
                d.model(), d.os(), d.lastSync(), d.status(), adj,
                d.lockSecure(), d.screenLockText(), d.encSecure(), d.encryptionText(),
                d.osSecure(), d.osText(), d.intSecure(), d.integrityText()
        );
    }

    public SecurityScoreBreakdownDto buildDevicesBreakdown(int totalDevices, int lockScreenCount, int encryptionCount, int osVersionCount, int integrityCount, int securityScore,
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

    public int calculateScore(boolean lock, boolean enc, boolean integrity, boolean os) {
        int score = 0;
        if (lock) score += 25;
        if (enc) score += 25;
        if (integrity) score += 25;
        if (os) score += 25;
        return score;
    }

    int adjustedDeviceComplianceScore(DeviceDetail device, boolean ignLock, boolean ignEnc, boolean ignOs, boolean ignInt) {
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
}
