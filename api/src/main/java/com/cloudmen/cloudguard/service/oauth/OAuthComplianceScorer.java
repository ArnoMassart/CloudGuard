package com.cloudmen.cloudguard.service.oauth;

import com.cloudmen.cloudguard.dto.password.SecurityScoreBreakdownDto;
import com.cloudmen.cloudguard.dto.password.SecurityScoreFactorDto;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.MessageSource;
import org.springframework.context.i18n.LocaleContextHolder;
import org.springframework.stereotype.Component;

import java.util.Locale;
import java.util.Set;

import static com.cloudmen.cloudguard.utility.GoogleServiceHelperMethods.securityScoreFactorForDetail;
import static com.cloudmen.cloudguard.utility.GoogleServiceHelperMethods.severity;

/**
 * Component responsible for evaluating the risk level of OAuth
 * applications and calculating overall compliance scores.
 */
@Component
public class OAuthComplianceScorer {
    private final MessageSource messageSource;

    public OAuthComplianceScorer(@Qualifier("messageSource") MessageSource messageSource) {
        this.messageSource = messageSource;
    }

    public boolean isAppHighRisk(Set<String> scopes) {
        return scopes.stream().anyMatch(scope ->
                scope.contains("/auth/drive") ||
                        scope.contains("/auth/gmail") ||
                        scope.contains("/auth/admin.directory") ||
                        scope.contains("/auth/cloud-platform")
        );
    }

    public int calculateSecurityScore(long totalThirdPartyApps, long totalHighRiskApps, boolean ignoreHighRiskPref) {
        if (ignoreHighRiskPref || totalThirdPartyApps == 0) {
            return 100;
        }
        double penalty = ((double) totalHighRiskApps / totalThirdPartyApps) * 100;
        return (int) Math.max(0, 100 - Math.round(penalty));
    }

    public SecurityScoreBreakdownDto buildOAuthBreakdown(long totalThirdPartyApps, long totalHighRiskApps, long totalPermissionsGranted, int securityScore,
                                                          boolean ignoreHighRiskPref) {
        int highRiskScore = totalThirdPartyApps == 0 ? 100 : (int) Math.round((totalThirdPartyApps - totalHighRiskApps) * 100.0 / totalThirdPartyApps);
        if (ignoreHighRiskPref) {
            highRiskScore = 100;
        }
        int noAppsScore = 100;

        Locale locale = LocaleContextHolder.getLocale();

        boolean showHighRiskFactor = totalThirdPartyApps > 0 || ignoreHighRiskPref;

        var factors = java.util.List.of(
                securityScoreFactorForDetail(
                        showHighRiskFactor,
                        messageSource.getMessage("apps.score.factor.without_high_risk.title", null, locale),
                        totalHighRiskApps == 0
                                ? messageSource.getMessage("apps.score.factor.without_high_risk.description.none", null, locale)
                                : messageSource.getMessage("apps.score.factor.without_high_risk.description", new Object[]{totalHighRiskApps, totalThirdPartyApps}, locale),
                        highRiskScore,
                        100,
                        severity(highRiskScore),
                        ignoreHighRiskPref),
                new SecurityScoreFactorDto("3rd-party apps", totalThirdPartyApps == 0 ? messageSource.getMessage("apps.score.factor.third_party.description.none", null, locale) : messageSource.getMessage("apps.score.factor.third_party.description", new Object[]{totalThirdPartyApps, totalPermissionsGranted}, locale), noAppsScore, 100, severity(noAppsScore), false)
        );
        String status = securityScore == 100 ? "perfect" : securityScore >= 75 ? "good" : securityScore > 50 ? "average" : "bad";
        return new SecurityScoreBreakdownDto(securityScore, status, factors);
    }
}
