package com.cloudmen.cloudguard.dto.dns;

import com.cloudmen.cloudguard.dto.password.SecurityScoreBreakdownDto;

import java.util.List;

/**
 * Aggregated DNS hygiene response for a single apex domain: checklist rows plus headline score and chart breakdown.
 *
 * @param domain                  apex hostname being audited (query parameter from controller)
 * @param rows                    ordered evaluations (SPF through mail CNAME)
 * @param securityScore           0–100 weighted over {@link com.cloudmen.cloudguard.domain.model.DnsRecordImportance#REQUIRED}
 *                              and {@link com.cloudmen.cloudguard.domain.model.DnsRecordImportance#RECOMMENDED} only
 * @param securityScoreBreakdown per-row factors mirroring password-settings breakdown DTOs
 */
public record DnsRecordResponseDto(
        String domain,
        List<DnsRecordDto> rows,
        int securityScore,
        SecurityScoreBreakdownDto securityScoreBreakdown
) {
}
