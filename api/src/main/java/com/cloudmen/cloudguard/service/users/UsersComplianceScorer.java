package com.cloudmen.cloudguard.service.users;

import com.cloudmen.cloudguard.dto.password.SecurityScoreBreakdownDto;
import com.cloudmen.cloudguard.utility.DateTimeConverter;
import com.google.api.client.util.DateTime;
import com.google.api.services.admin.directory.model.User;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.MessageSource;
import org.springframework.context.i18n.LocaleContextHolder;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Locale;

import static com.cloudmen.cloudguard.utility.GoogleServiceHelperMethods.securityScoreFactorForDetail;
import static com.cloudmen.cloudguard.utility.GoogleServiceHelperMethods.severity;

/**
 * Component responsible for calculating compliance scores and analyzing security risks associated with user
 * accounts, such as missing 2FA or abnormal login activity.
 */
@Component
public class UsersComplianceScorer {
    private final MessageSource messageSource;

    public UsersComplianceScorer(@Qualifier("messageSource") MessageSource messageSource) {
        this.messageSource = messageSource;
    }

    public int calculateSecurityScoreWithPreferenceMask(List<User> googleUsers) {
        if (googleUsers.isEmpty()) {
            return 0;
        }

        long sum = 0;
        int totalParts = googleUsers.size() * 2;

        for (User user : googleUsers) {
            boolean isActive = !Boolean.TRUE.equals(user.getSuspended());
            boolean twoFAEnabled = Boolean.TRUE.equals(user.getIsEnrolledIn2Sv());

            sum += (!isActive || twoFAEnabled) ? 1 : 0;

            sum += activityMeasuresComply(isActive, user.getLastLoginTime()) ? 1 : 0;
        }

        return (int) Math.floor((double) sum / totalParts * 100);
    }

    /**
     * Each non-compliant user is attributed to exactly one failure reason (in order of checks).
     */
    public SecurityScoreBreakdownDto buildUsersBreakdown(List<User> googleUsers, int totalUsers, int securityScore) {
        LocalDate now = LocalDate.now();
        int no2FACount = 0;
        int longNoLoginCount = 0;
        int inactiveRecentCount = 0;

        for (User user : googleUsers) {
            boolean isActive = !Boolean.TRUE.equals(user.getSuspended());
            boolean twoFAEnabled = Boolean.TRUE.equals(user.getIsEnrolledIn2Sv());
            DateTime lastLogin = user.getLastLoginTime();

            if (isActive && !twoFAEnabled) {
                no2FACount++;
                continue;
            }
            if (isActive) {
                if (lastLogin == null) {
                    longNoLoginCount++;
                } else {
                    LocalDate loginDate = DateTimeConverter.convertGoogleDateTimeToLocalDate(lastLogin);
                    if (ChronoUnit.DAYS.between(loginDate, now) >= 90) {
                        longNoLoginCount++;
                    }
                }
                continue;
            }
            if (lastLogin != null) {
                LocalDate loginDate = DateTimeConverter.convertGoogleDateTimeToLocalDate(lastLogin);
                if (ChronoUnit.DAYS.between(loginDate, now) <= 7) {
                    inactiveRecentCount++;
                }
            }
        }

        int score1 = totalUsers == 0 ? 100 : (int) Math.round(100.0 * (totalUsers - no2FACount) / totalUsers);
        int score2 = totalUsers == 0 ? 100 : (int) Math.round(100.0 * (totalUsers - longNoLoginCount) / totalUsers);
        int score3 = totalUsers == 0 ? 100 : (int) Math.round(100.0 * (totalUsers - inactiveRecentCount) / totalUsers);

        Locale locale = LocaleContextHolder.getLocale();

        boolean show2faFactor = totalUsers > 0;
        boolean showActivityFactors = totalUsers > 0;

        var factors = java.util.List.of(
                securityScoreFactorForDetail(
                        show2faFactor,
                        messageSource.getMessage("users.overview.2step-title", null, locale),
                        no2FACount == 0 ? messageSource.getMessage("users.overview.2step.compliant", null, locale) : messageSource.getMessage("users.overview.2step.non_compliant", new Object[]{no2FACount}, locale),
                        score1,
                        100,
                        severity(score1)
                        ),
                securityScoreFactorForDetail(
                        showActivityFactors,
                        messageSource.getMessage("users.overview.activeLongNoLogin-title", null, locale),
                        longNoLoginCount == 0 ? messageSource.getMessage("users.overview.no_login.compliant", null, locale) : messageSource.getMessage("users.overview.no_login.non_compliant", new Object[]{longNoLoginCount}, locale),
                        score2,
                        100,
                        severity(score2)
                        ),
                securityScoreFactorForDetail(
                        showActivityFactors,
                        messageSource.getMessage("users.overview.deactivatedRecentLogin-title", null, locale),
                        inactiveRecentCount == 0 ? messageSource.getMessage("users.overview.recent_login.compliant", null, locale) : messageSource.getMessage("users.overview.recent_login.non_compliant", new Object[]{inactiveRecentCount}, locale),
                        score3,
                        100,
                        severity(score3))
        );
        String status = securityScore == 100 ? "perfect" : securityScore >= 75 ? "good" : securityScore > 50 ? "average" : "bad";
        return new SecurityScoreBreakdownDto(securityScore, status, factors);
    }

    private static boolean activityMeasuresComply(boolean isActive, DateTime lastLogin) {
        LocalDate now = LocalDate.now();
        if (isActive) {
            if (lastLogin == null) {
                return false;
            }
            LocalDate loginDate = DateTimeConverter.convertGoogleDateTimeToLocalDate(lastLogin);
            return ChronoUnit.YEARS.between(loginDate, now) < 1;
        }
        if (lastLogin == null) {
            return true;
        }
        LocalDate loginDate = DateTimeConverter.convertGoogleDateTimeToLocalDate(lastLogin);
        return ChronoUnit.DAYS.between(loginDate, now) > 7;
    }


}
