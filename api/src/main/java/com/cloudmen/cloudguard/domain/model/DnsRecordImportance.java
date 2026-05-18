package com.cloudmen.cloudguard.domain.model;

/**
 * How strongly a DNS check should influence scoring and {@link DnsRecordStatus} when records are missing or weak.
 * User overrides arrive via {@link com.cloudmen.cloudguard.service.preference.UserSecurityPreferenceService#getDnsImportanceOverrides(String)}.
 */
public enum DnsRecordImportance {
    /** Strong impact on score and usually maps to {@link DnsRecordStatus#ACTION_REQUIRED} when absent. */
    REQUIRED,
    /** Moderate score weight; missing rows tend toward {@link DnsRecordStatus#ATTENTION}. */
    RECOMMENDED,
    /** Informational only; contributes muted breakdown rows and does not reduce headline score. */
    OPTIONAL
}
