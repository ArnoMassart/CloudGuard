package com.cloudmen.cloudguard.dto.preferences;

import java.util.Map;

public record SectionWarningsDto(
        Map<String, Boolean> items,
        boolean hasWarnings,
        boolean hasMultipleWarnings
) {}
