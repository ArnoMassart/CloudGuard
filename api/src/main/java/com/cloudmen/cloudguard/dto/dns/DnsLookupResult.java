package com.cloudmen.cloudguard.dto.dns;

import java.util.List;

/**
 * Result of a DNS lookup. Distinguishes between successful lookup (possibly empty RRs)
 * and lookup failure (resolver error, bad name, etc.).
 *
 * @param values       TXT/MX/CNAME/CAA/DNSKEY payload strings; empty when none found or on failure
 * @param errorMessage {@code null} on success; otherwise resolver failure reason for UI / {@link com.cloudmen.cloudguard.domain.model.DnsRecordStatus#ERROR}
 */
public record DnsLookupResult(List<String> values, String errorMessage) {

    public static DnsLookupResult success(List<String> values) {
        return new DnsLookupResult(values != null ? values : List.of(), null);
    }

    public static DnsLookupResult failure(String errorMessage) {
        return new DnsLookupResult(List.of(), errorMessage);
    }

    public boolean isSuccess() {
        return errorMessage == null;
    }
}
