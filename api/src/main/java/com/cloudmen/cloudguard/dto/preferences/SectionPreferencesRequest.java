package com.cloudmen.cloudguard.dto.preferences;

import java.util.Map;

public record SectionPreferencesRequest(String section, Map<String, Boolean> preferences) {}
