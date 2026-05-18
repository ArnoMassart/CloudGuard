package com.cloudmen.cloudguard.dto.preferences;

import java.util.Map;

/**
 * Module overview warning strip: which warning keys are active after applying disabled preference keys.
 *
 * @param items                  logical warning id → visible when {@code true}
 * @param hasWarnings            any active warning
 * @param hasMultipleWarnings    more than one active warning (UI severity / layout)
 */
public record SectionWarningsDto(
        Map<String, Boolean> items,
        boolean hasWarnings,
        boolean hasMultipleWarnings
) {}
