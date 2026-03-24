package com.cloudmen.cloudguard.dto.preferences;

public record SetPreferenceRequest(String section, String preferenceKey, boolean enabled) {}
