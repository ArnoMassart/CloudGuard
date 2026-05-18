package com.cloudmen.cloudguard.dto.groups;

import com.cloudmen.cloudguard.dto.password.SecurityScoreBreakdownDto;
import com.cloudmen.cloudguard.dto.preferences.SectionWarningsDto;

/**
 * Workspace-wide groups snapshot for overview APIs: counts by external membership and policy, risk-tier buckets,
 * optional headline security score with translated breakdown rows, and preference-driven warnings.
 *
 * @param totalGroups                 groups in the cached tenant snapshot
 * @param groupsWithExternal          groups with at least one external USER member ({@link GroupOrgDetail#getExternalMembers()} {@code > 0})
 * @param groupsWithExternalAllowed   groups whose settings allow external members ({@link GroupOrgDetail#isExternalAllowed()})
 * @param highRiskGroups              {@link GroupOrgDetail#getRisk()} {@code HIGH}
 * @param mediumRiskGroups            risk {@code MEDIUM}
 * @param lowRiskGroups               risk {@code LOW} or any value outside HIGH/MEDIUM
 * @param securityScore               weighted 0–100 score, or {@code null} when {@code totalGroups == 0}
 * @param securityScoreBreakdown      factor rows for the score card, or {@code null}
 * @param warnings                    which overview checks are affected by disabled org preferences, or {@code null}
 */
public record GroupOverviewResponse(
        int totalGroups,
        int groupsWithExternal,
        int groupsWithExternalAllowed,
        int highRiskGroups,
        int mediumRiskGroups,
        int lowRiskGroups,
        Integer securityScore,
        SecurityScoreBreakdownDto securityScoreBreakdown,
        SectionWarningsDto warnings
) {}
