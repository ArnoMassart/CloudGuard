package com.cloudmen.cloudguard.service.preference;

import com.cloudmen.cloudguard.domain.model.DnsRecordImportance;
import com.cloudmen.cloudguard.domain.model.preference.UserSecurityPreference;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * DNS record types checked in {@link com.cloudmen.cloudguard.service.dns.DnsRecordsService},
 * their system-default importance, and preference keys under section {@code domain-dns}.
 */
public final class DnsImportancePreferenceSupport {

    private DnsImportancePreferenceSupport() {}

    public static final List<String> DNS_TYPES = List.of(
            "SPF", "DKIM", "DMARC", "MX", "DNSSEC", "CAA", "TXT", "CNAME"
    );

    private static final Map<String, DnsRecordImportance> SYSTEM_DEFAULTS = Map.ofEntries(
            Map.entry("SPF", DnsRecordImportance.REQUIRED),
            Map.entry("DKIM", DnsRecordImportance.REQUIRED),
            Map.entry("DMARC", DnsRecordImportance.REQUIRED),
            Map.entry("MX", DnsRecordImportance.REQUIRED),
            Map.entry("DNSSEC", DnsRecordImportance.RECOMMENDED),
            Map.entry("CAA", DnsRecordImportance.RECOMMENDED),
            Map.entry("TXT", DnsRecordImportance.OPTIONAL),
            Map.entry("CNAME", DnsRecordImportance.OPTIONAL)
    );

    private static final Map<String, String> TYPE_TO_PREF_KEY = Map.ofEntries(
            Map.entry("SPF", "impSpf"),
            Map.entry("DKIM", "impDkim"),
            Map.entry("DMARC", "impDmarc"),
            Map.entry("MX", "impMx"),
            Map.entry("DNSSEC", "impDnssec"),
            Map.entry("CAA", "impCaa"),
            Map.entry("TXT", "impTxt"),
            Map.entry("CNAME", "impCname")
    );

    public static String preferenceKeyForType(String dnsType) {
        return TYPE_TO_PREF_KEY.get(dnsType);
    }

    public static DnsRecordImportance systemDefaultImportance(String dnsType) {
        return SYSTEM_DEFAULTS.getOrDefault(dnsType, DnsRecordImportance.OPTIONAL);
    }

    public static Map<String, DnsRecordImportance> systemDefaultsMap() {
        return new LinkedHashMap<>(SYSTEM_DEFAULTS);
    }

    public static boolean isDnsImportancePreferenceKey(String preferenceKey) {
        return preferenceKey != null && preferenceKey.startsWith("imp");
    }

    public static Map<String, DnsRecordImportance> parseOverridesFromDbRows(List<UserSecurityPreference> rows) {
        Map<String, DnsRecordImportance> out = new LinkedHashMap<>();
        for (var p : rows) {
            if (!"domain-dns".equals(p.getSection()) || !isDnsImportancePreferenceKey(p.getPreferenceKey())) {
                continue;
            }
            String v = p.getPreferenceValue();
            if (v == null || v.isBlank()) {
                continue;
            }
            String type = dnsTypeForPreferenceKey(p.getPreferenceKey());
            if (type != null) {
                try {
                    out.put(type, DnsRecordImportance.valueOf(v.trim()));
                } catch (IllegalArgumentException ignored) {
                    // skip invalid stored value
                }
            }
        }
        return out;
    }

    public static String dnsTypeForPreferenceKey(String preferenceKey) {
        for (var e : TYPE_TO_PREF_KEY.entrySet()) {
            if (e.getValue().equals(preferenceKey)) {
                return e.getKey();
            }
        }
        return null;
    }
}
