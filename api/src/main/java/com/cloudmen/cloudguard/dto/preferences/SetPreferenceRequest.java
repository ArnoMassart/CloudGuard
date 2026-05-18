package com.cloudmen.cloudguard.dto.preferences;

/**
 * PUT body for {@link com.cloudmen.cloudguard.controller.UserSecurityPreferenceController#setPreference}.
 *
 * @param section        settings section (e.g. {@code password-settings}, {@code domain-dns})
 * @param preferenceKey  boolean toggle key or DNS {@code imp*} key
 * @param enabled        when {@code null} and {@code value} is set, only DNS importance is updated
 * @param value          for {@code domain-dns} {@code imp*} keys: REQUIRED, RECOMMENDED, OPTIONAL; blank clears override
 */
public record SetPreferenceRequest(String section, String preferenceKey, Boolean enabled, String value) {}
