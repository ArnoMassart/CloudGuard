package com.cloudmen.cloudguard.dto.preferences;

import java.util.Map;

/**
 * Bulk boolean updates for one {@code section} ({@code PUT /user/preferences/section}).
 *
 * @param section      target module section
 * @param preferences  {@code preferenceKey} → enabled (DNS {@code imp*} keys not supported here)
 */
public record SectionPreferencesRequest(String section, Map<String, Boolean> preferences) {}
