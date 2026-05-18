package com.cloudmen.cloudguard.domain.model;

/**
 * UI-facing outcome for a checked DNS row after combining {@link DnsLookupResult}, presence/optimal heuristics,
 * and {@link DnsRecordImportance}.
 */
public enum DnsRecordStatus {
    /** Present and matches recommended posture (e.g. SPF includes Google when expected). */
    VALID,
    /** Acceptable for optional checks or non-blocking gaps. */
    OK,
    /** Present but suboptimal or soft failure worth highlighting. */
    ATTENTION,
    /** Missing or broken where {@link DnsRecordImportance#REQUIRED} applies. */
    ACTION_REQUIRED,
    /** Resolver or transport error ({@link com.cloudmen.cloudguard.dto.dns.DnsLookupResult#errorMessage()} set). */
    ERROR
}
