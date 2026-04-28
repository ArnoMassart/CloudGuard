package com.cloudmen.cloudguard.service.drives;

import com.cloudmen.cloudguard.dto.password.SecurityScoreBreakdownDto;
import com.cloudmen.cloudguard.dto.password.SecurityScoreFactorDto;
import com.cloudmen.cloudguard.service.preference.SecurityPreferenceScoreSupport;
import com.cloudmen.cloudguard.utility.GoogleServiceHelperMethods;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.MessageSource;
import org.springframework.context.i18n.LocaleContextHolder;
import org.springframework.stereotype.Component;

import java.util.Locale;
import java.util.Set;

import static com.cloudmen.cloudguard.utility.GoogleServiceHelperMethods.securityScoreFactorForDetail;
import static com.cloudmen.cloudguard.utility.GoogleServiceHelperMethods.severity;

@Component
public class DrivesComplianceScorer {
    private final MessageSource messageSource;

    public DrivesComplianceScorer(@Qualifier("messageSource") MessageSource messageSource) {
        this.messageSource = messageSource;
    }

    public SecurityScoreBreakdownDto buildDrivesBreakdown(int totalDrives, int totalLowRisk, int totalMediumRisk, int totalHighRisk,
                                                          int orphanDrives, int notOnlyDomainUsersAllowedCount,
                                                          int notOnlyMembersCanAccessCount,
                                                          int riskOnlyScore, Set<String> off) {
        int lowScore = GoogleServiceHelperMethods.calculateWeightedScore(totalDrives, totalLowRisk, 100.0, 100);
        int mediumScore = GoogleServiceHelperMethods.calculateWeightedScore(totalDrives, totalMediumRisk, 60.0, 0);
        String mediumSeverity = severity(mediumScore > 0 ? mediumScore * 100.0 / 60 : 0, "warning");
        int highScore = GoogleServiceHelperMethods.calculateWeightedScore(totalDrives, totalHighRisk, 20.0, 0);
        String highSeverity = severity(highScore > 0 ? highScore * 100.0 / 20 : 0, true);

        boolean muteRisk = SecurityPreferenceScoreSupport.preferenceDisabled(off, "shared-drives", "external");
        boolean muteOrphan = SecurityPreferenceScoreSupport.preferenceDisabled(off, "shared-drives", "orphan");
        boolean muteDomain = SecurityPreferenceScoreSupport.preferenceDisabled(off, "shared-drives", "outsideDomain");
        boolean muteMembers = SecurityPreferenceScoreSupport.preferenceDisabled(off, "shared-drives", "nonMemberAccess");

        /* Same as groups: omit a risk tier from the detail list when count in that tier is 0 (avoids red 0/N). */
        boolean showLowTier = muteRisk || (totalDrives > 0 && totalLowRisk > 0);
        boolean showMediumTier = muteRisk || (totalDrives > 0 && totalMediumRisk > 0);
        boolean showHighTier = muteRisk || (totalDrives > 0 && totalHighRisk > 0);
        int orphanScore = GoogleServiceHelperMethods.calculateDeductionScore(totalDrives, orphanDrives);
        int domainOnlyScore = totalDrives == 0 ? 100 : notOnlyDomainUsersAllowedCount == 0 ? 100 : (int) Math.max(0, 100 - notOnlyDomainUsersAllowedCount * 50 / totalDrives);
        int membersOnlyScore = totalDrives == 0 ? 100 : notOnlyMembersCanAccessCount == 0 ? 100 : (int) Math.max(0, 100 - notOnlyMembersCanAccessCount * 50 / totalDrives);

        if (SecurityPreferenceScoreSupport.preferenceDisabled(off, "shared-drives", "orphan")) {
            orphanScore = 100;
        }
        if (SecurityPreferenceScoreSupport.preferenceDisabled(off, "shared-drives", "outsideDomain")) {
            domainOnlyScore = 100;
        }
        if (SecurityPreferenceScoreSupport.preferenceDisabled(off, "shared-drives", "nonMemberAccess")) {
            membersOnlyScore = 100;
        }

        int combinedScore = combinedDriveSecurityScore(totalDrives, riskOnlyScore, orphanScore, domainOnlyScore, membersOnlyScore, off);

        Locale locale = LocaleContextHolder.getLocale();

        var factors = java.util.List.of(
                securityScoreFactorForDetail(
                        showLowTier,
                        messageSource.getMessage("drives.score.factor.low_risk.title", null, locale),
                        messageSource.getMessage("drives.score.factor.low_risk.description", new Object[]{totalLowRisk, totalDrives}, locale),
                        lowScore,
                        100,
                        severity(lowScore, true),
                        muteRisk),
                securityScoreFactorForDetail(
                        showMediumTier,
                        messageSource.getMessage("drives.score.factor.middle_risk.title", null, locale),
                        messageSource.getMessage("drives.score.factor.middle_risk.description", new Object[]{totalMediumRisk, totalDrives}, locale),
                        mediumScore,
                        60,
                        mediumSeverity,
                        muteRisk),
                securityScoreFactorForDetail(
                        showHighTier,
                        messageSource.getMessage("drives.score.factor.high_risk.title", null, locale),
                        messageSource.getMessage("drives.score.factor.high_risk.description", new Object[]{totalHighRisk, totalDrives}, locale),
                        highScore,
                        20,
                        highSeverity,
                        muteRisk),
                new SecurityScoreFactorDto(messageSource.getMessage("drives.score.factor.with_managers.title", null, locale), orphanDrives == 0 ? messageSource.getMessage("drives.score.factor.with_managers_not.description", null, locale) : messageSource.getMessage("drives.score.factor.with_managers.description", new Object[]{orphanDrives}, locale), orphanScore, 100, severity(orphanScore), muteOrphan),
                new SecurityScoreFactorDto(messageSource.getMessage("drives.score.factor.only_domain.title", null, locale), notOnlyDomainUsersAllowedCount == 0 ? messageSource.getMessage("drives.score.factor.only_domain_not.description", null, locale) : messageSource.getMessage("drives.score.factor.only_domain.description", new Object[]{notOnlyDomainUsersAllowedCount}, locale), domainOnlyScore, 100, severity(domainOnlyScore), muteDomain),
                new SecurityScoreFactorDto(messageSource.getMessage("drives.score.factor.only_members.title", null, locale), notOnlyMembersCanAccessCount == 0 ? messageSource.getMessage("drives.score.factor.only_members_not.description", null, locale) : messageSource.getMessage("drives.score.factor.only_members.description", new Object[]{notOnlyMembersCanAccessCount}, locale), membersOnlyScore, 100, severity(membersOnlyScore), muteMembers)
        );
        String status = combinedScore == 100 ? "perfect" : combinedScore >= 75 ? "good" : combinedScore > 50 ? "average" : "bad";
        return new SecurityScoreBreakdownDto(combinedScore, status, factors);
    }

    private int combinedDriveSecurityScore(int totalDrives, int riskOnlyScore, int orphanScore,
                                                  int domainOnlyScore, int membersOnlyScore, Set<String> off) {
        if (totalDrives == 0) {
            return 0;
        }
        double sum = 0;
        int parts = 0;
        if (!SecurityPreferenceScoreSupport.preferenceDisabled(off, "shared-drives", "external")) {
            sum += riskOnlyScore;
            parts++;
        }
        if (!SecurityPreferenceScoreSupport.preferenceDisabled(off, "shared-drives", "orphan")) {
            sum += orphanScore;
            parts++;
        }
        if (!SecurityPreferenceScoreSupport.preferenceDisabled(off, "shared-drives", "outsideDomain")) {
            sum += domainOnlyScore;
            parts++;
        }
        if (!SecurityPreferenceScoreSupport.preferenceDisabled(off, "shared-drives", "nonMemberAccess")) {
            sum += membersOnlyScore;
            parts++;
        }
        if (parts == 0) {
            return 100;
        }
        return (int) Math.round(sum / parts);
    }
}
