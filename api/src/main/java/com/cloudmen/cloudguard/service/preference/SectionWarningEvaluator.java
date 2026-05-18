package com.cloudmen.cloudguard.service.preference;

import com.cloudmen.cloudguard.dto.preferences.SectionWarningsDto;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;

/**
 * Fluent builder for per-section warning flags: a warning is active when {@code count > 0} and the linked preference
 * is not in {@code disabledKeys}.
 *
 * @see SecurityPreferenceScoreSupport#preferenceDisabled(Set, String, String)
 */
public final class SectionWarningEvaluator {

    private final Map<String, Boolean> items = new LinkedHashMap<>();
    private final Set<String> disabledKeys;

    private SectionWarningEvaluator(Set<String> disabledKeys) {
        this.disabledKeys = disabledKeys;
    }

    /** Starts evaluation with the caller’s disabled preference keys ({@code section:key}). */
    public static SectionWarningEvaluator with(Set<String> disabledKeys) {
        return new SectionWarningEvaluator(disabledKeys);
    }

    /**
     * Registers {@code warningKey} as active when {@code count > 0} and {@code section:prefKey} is not disabled.
     */
    public SectionWarningEvaluator check(String warningKey, long count, String section, String prefKey) {
        boolean active = count > 0 && !disabledKeys.contains(section + ":" + prefKey);
        items.put(warningKey, active);
        return this;
    }

    /** Aggregates item map plus coarse {@code hasWarnings} / {@code hasMultipleWarnings} flags for section DTOs. */
    public SectionWarningsDto build() {
        long activeCount = items.values().stream().filter(Boolean::booleanValue).count();
        return new SectionWarningsDto(
                Map.copyOf(items),
                activeCount > 0,
                activeCount > 1
        );
    }
}
