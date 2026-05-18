package com.cloudmen.cloudguard.dto.preferences;

import java.util.Map;
import java.util.Set;

/**
 * Full preferences payload for the security settings UI.
 *
 * @param preferences                  {@code section:preferenceKey} → enabled (defaults {@code true} when no DB row)
 * @param dnsImportance                DNS type → effective {@link com.cloudmen.cloudguard.domain.model.DnsRecordImportance} name
 * @param dnsImportanceOverrideTypes   DNS types where an explicit {@code preference_value} row exists (UI emphasis)
 */
public record PreferencesResponse(
        Map<String, Boolean> preferences,
        Map<String, String> dnsImportance,
        Set<String> dnsImportanceOverrideTypes
) {}
