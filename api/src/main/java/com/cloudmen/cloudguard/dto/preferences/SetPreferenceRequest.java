package com.cloudmen.cloudguard.dto.preferences;

/**
 * @param enabled when null and {@code value} is set, only DNS importance is updated.
 * @param value     for domain-dns imp* keys: REQUIRED, RECOMMENDED, OPTIONAL; empty clears override.
 */
public record SetPreferenceRequest(String section, String preferenceKey, Boolean enabled, String value) {}
