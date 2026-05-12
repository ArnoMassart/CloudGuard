package com.cloudmen.cloudguard.service.drives;

import com.cloudmen.cloudguard.dto.password.SecurityScoreBreakdownDto;
import com.cloudmen.cloudguard.dto.password.SecurityScoreFactorDto;
import com.cloudmen.cloudguard.utility.GoogleServiceHelperMethods;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.MessageSource;
import org.springframework.context.i18n.LocaleContextHolder;
import org.springframework.stereotype.Component;

import java.util.Locale;

import static com.cloudmen.cloudguard.utility.GoogleServiceHelperMethods.*;

/**
 * Component responsible for calculating compliance scores and evaluating the risk levels of Google Workspace Shared
 * Drives. <p>
 *
 * This scorer handles the complex weighting of risk tiers (low, medium, high), applies deductions for unsafe
 * sharing settings (e.g., orphan drives, external access), and generates the final breakdown.
 */
@Component
public class DrivesComplianceScorer {
    private final MessageSource messageSource;

    public DrivesComplianceScorer(@Qualifier("messageSource") MessageSource messageSource) {
        this.messageSource = messageSource;
    }

    public SecurityScoreBreakdownDto buildDrivesBreakdown(int totalDrives, int totalLowRisk, int totalMediumRisk, int totalHighRisk,
                                                          int orphanDrives, int notOnlyDomainUsersAllowedCount,
                                                          int notOnlyMembersCanAccessCount,
                                                          int riskOnlyScore) {

        int lowScore = GoogleServiceHelperMethods.calculateWeightedScore(totalDrives, totalLowRisk, 100.0, 100);
        int mediumScore = GoogleServiceHelperMethods.calculateWeightedScore(totalDrives, totalMediumRisk, 60.0, 0);
        String mediumSeverity = severity(mediumScore > 0 ? mediumScore * 100.0 / 60 : 0, "warning");
        int highScore = GoogleServiceHelperMethods.calculateWeightedScore(totalDrives, totalHighRisk, 20.0, 0);
        String highSeverity = severity(highScore > 0 ? highScore * 100.0 / 20 : 0, true);

        // Omit a risk tier from the detail list when count in that tier is 0 (avoids red 0/N).
        boolean showLowTier = (totalDrives > 0 && totalLowRisk > 0);
        boolean showMediumTier = (totalDrives > 0 && totalMediumRisk > 0);
        boolean showHighTier = (totalDrives > 0 && totalHighRisk > 0);

        int orphanScore = GoogleServiceHelperMethods.calculateDeductionScore(totalDrives, orphanDrives);
        int domainOnlyScore = totalDrives == 0 ? 100 : notOnlyDomainUsersAllowedCount == 0 ? 100 : (int) Math.max(0, 100 - notOnlyDomainUsersAllowedCount * 50 / totalDrives);
        int membersOnlyScore = totalDrives == 0 ? 100 : notOnlyMembersCanAccessCount == 0 ? 100 : (int) Math.max(0, 100 - notOnlyMembersCanAccessCount * 50 / totalDrives);

        int combinedScore = combinedDriveSecurityScore(totalDrives, riskOnlyScore, orphanScore, domainOnlyScore, membersOnlyScore);

        Locale locale = LocaleContextHolder.getLocale();

        var factors = java.util.List.of(
                securityScoreFactorForDetail(
                        showLowTier,
                        messageSource.getMessage("drives.score.factor.low_risk.title", null, locale),
                        messageSource.getMessage("drives.score.factor.low_risk.description", new Object[]{totalLowRisk, totalDrives}, locale),
                        lowScore,
                        100,
                        severity(lowScore, true)
                ),
                securityScoreFactorForDetail(
                        showMediumTier,
                        messageSource.getMessage("drives.score.factor.middle_risk.title", null, locale),
                        messageSource.getMessage("drives.score.factor.middle_risk.description", new Object[]{totalMediumRisk, totalDrives}, locale),
                        mediumScore,
                        60,
                        mediumSeverity),
                securityScoreFactorForDetail(
                        showHighTier,
                        messageSource.getMessage("drives.score.factor.high_risk.title", null, locale),
                        messageSource.getMessage("drives.score.factor.high_risk.description", new Object[]{totalHighRisk, totalDrives}, locale),
                        highScore,
                        20,
                        highSeverity),
                new SecurityScoreFactorDto(
                        messageSource.getMessage("drives.score.factor.with_managers.title", null, locale),
                        orphanDrives == 0 ? messageSource.getMessage("drives.score.factor.with_managers_not.description", null, locale) : messageSource.getMessage("drives.score.factor.with_managers.description", new Object[]{orphanDrives}, locale),
                        orphanScore, 100, severity(orphanScore), false),
                new SecurityScoreFactorDto(
                        messageSource.getMessage("drives.score.factor.only_domain.title", null, locale),
                        notOnlyDomainUsersAllowedCount == 0 ? messageSource.getMessage("drives.score.factor.only_domain_not.description", null, locale) : messageSource.getMessage("drives.score.factor.only_domain.description", new Object[]{notOnlyDomainUsersAllowedCount}, locale),
                        domainOnlyScore, 100, severity(domainOnlyScore), false),
                new SecurityScoreFactorDto(
                        messageSource.getMessage("drives.score.factor.only_members.title", null, locale),
                        notOnlyMembersCanAccessCount == 0 ? messageSource.getMessage("drives.score.factor.only_members_not.description", null, locale) : messageSource.getMessage("drives.score.factor.only_members.description", new Object[]{notOnlyMembersCanAccessCount}, locale),
                        membersOnlyScore, 100, severity(membersOnlyScore), false)
        );
        String status = getOverviewStatus(combinedScore);
        return new SecurityScoreBreakdownDto(combinedScore, status, factors);
    }

    private int combinedDriveSecurityScore(int totalDrives, int riskOnlyScore, int orphanScore,
                                           int domainOnlyScore, int membersOnlyScore) {
        if (totalDrives == 0) {
            return 100; // Als er 0 drives zijn, is de score 100% veilig (want er is geen risico)
        }

        // Tel alle 4 de scores bij elkaar op en deel door 4
        double sum = riskOnlyScore + orphanScore + domainOnlyScore + membersOnlyScore;
        return (int) Math.round(sum / 4.0);
    }
}