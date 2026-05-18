package com.cloudmen.cloudguard.dto.preferences;

/**
 * Single boolean preference row shape (legacy or auxiliary APIs).
 *
 * @param section        settings section
 * @param preferenceKey  key within the section
 * @param enabled        stored toggle
 */
public record UserPreferenceDto(String section, String preferenceKey, boolean enabled) {}
