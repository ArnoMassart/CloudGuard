package com.cloudmen.cloudguard.dto.preferences;

import java.util.Map;
import java.util.Set;

/**
 * Boolean toggles (section:key → enabled) plus per–DNS-type effective importance (SPF → REQUIRED, …).
 *
 * @param dnsImportanceOverrideTypes DNS types (SPF, …) the user has explicitly stored an importance for.
 */
public record PreferencesResponse(
        Map<String, Boolean> preferences,
        Map<String, String> dnsImportance,
        Set<String> dnsImportanceOverrideTypes
) {}
