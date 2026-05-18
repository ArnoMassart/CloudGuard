package com.cloudmen.cloudguard.dto.dns;

import com.cloudmen.cloudguard.domain.model.DnsRecordImportance;
import com.cloudmen.cloudguard.domain.model.DnsRecordStatus;

import java.util.List;

/**
 * One evaluated DNS row (SPF, DKIM, MX, …) for the domain/DNS module.
 *
 * @param type        logical check key ({@code SPF}, {@code DKIM}, {@code TXT}, …) aligned with {@link com.cloudmen.cloudguard.service.preference.DnsImportancePreferenceSupport}
 * @param name        queried FQDN (apex domain, {@code _dmarc.…}, selector host, {@code mail.…}, etc.)
 * @param values      raw RDATA strings from {@link DnsLookupService}
 * @param status      derived posture for badges and scoring
 * @param importance  weighting tier (possibly user-overridden)
 * @param message     localized explanation for tooltips / breakdown
 */
public record DnsRecordDto(
        String type,
        String name,
        List<String> values,
        DnsRecordStatus status,
        DnsRecordImportance importance,
        String message
) {
}
