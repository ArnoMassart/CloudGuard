package com.cloudmen.cloudguard.service.preference;

import com.cloudmen.cloudguard.dto.preferences.SectionWarningsDto;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;

/**
 * Evaluates per-section warnings by checking counts against user preferences.
 * A warning is active when count > 0 AND the preference is not disabled.
 */
public final class SectionWarningEvaluator {

    private final Map<String, Boolean> items = new LinkedHashMap<>();
    private final Set<String> disabledKeys;

    private SectionWarningEvaluator(Set<String> disabledKeys) {
        this.disabledKeys = disabledKeys;
    }

    public static SectionWarningEvaluator with(Set<String> disabledKeys) {
        return new SectionWarningEvaluator(disabledKeys);
    }

    public SectionWarningEvaluator check(String warningKey, long count, String section, String prefKey) {
        boolean active = count > 0 && !disabledKeys.contains(section + ":" + prefKey);
        items.put(warningKey, active);
        return this;
    }

    public SectionWarningsDto build() {
        long activeCount = items.values().stream().filter(Boolean::booleanValue).count();
        return new SectionWarningsDto(
                Map.copyOf(items),
                activeCount > 0,
                activeCount > 1
        );
    }
}
