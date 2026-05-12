package com.cloudmen.cloudguard.service.device;

import com.cloudmen.cloudguard.dto.devices.DeviceDetail;
import com.cloudmen.cloudguard.dto.password.SecurityScoreBreakdownDto;
import com.cloudmen.cloudguard.service.preference.SecurityPreferenceScoreSupport;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.MessageSource;
import org.springframework.context.i18n.LocaleContextHolder;
import org.springframework.stereotype.Component;

import java.util.Locale;
import java.util.Set;

import static com.cloudmen.cloudguard.utility.GoogleServiceHelperMethods.*;

@Component
public class DeviceComplianceScorer {
    private final MessageSource messageSource;

    public DeviceComplianceScorer(@Qualifier("messageSource") MessageSource messageSource) {
        this.messageSource = messageSource;
    }

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

        Locale locale = LocaleContextHolder.getLocale();

        var factors = java.util.List.of(
                securityScoreFactorForDetail(
                        show,
                        getMessage(messageSource, "devices.score.factor.lock_screen.title", locale),
                        lockScreenCount == 0 ? getMessage(messageSource, "devices.score.factor.lock_screen.description.all", locale) : getMessage(messageSource, "devices.score.factor.lock_screen.description", new Object[]{lockScreenCount}, locale),
                        lockScore,
                        100,
                        severity(lockScore)),
                securityScoreFactorForDetail(
                        show,
                        getMessage(messageSource, "devices.score.factor.enc.title", locale),
                        encryptionCount == 0 ? getMessage(messageSource, "devices.score.factor.enc.description.all", locale) : getMessage(messageSource, "devices.score.factor.enc.description", new Object[]{encryptionCount}, locale),
                        encScore,
                        100,
                        severity(encScore)),
                securityScoreFactorForDetail(
                        show,
                        getMessage(messageSource, "devices.score.factor.os_version.title", locale),
                        osVersionCount == 0 ? getMessage(messageSource, "devices.score.factor.os_version.description.all", locale) : getMessage(messageSource, "devices.score.factor.os_version.description", new Object[]{osVersionCount}, locale),
                        osScore,
                        100,
                        severity(osScore)),
                securityScoreFactorForDetail(
                        show,
                        getMessage(messageSource, "devices.score.factor.int.title", locale),
                        integrityCount == 0 ? getMessage(messageSource, "devices.score.factor.int.description.none", locale) : getMessage(messageSource, "devices.score.factor.int.description", new Object[]{integrityCount}, locale),
                        intScore,
                        100,
                        severity(intScore))
        );
        String status = securityScore == 100 ? "perfect" : securityScore >= 75 ? "good" : securityScore > 50 ? "average" : "bad";
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
