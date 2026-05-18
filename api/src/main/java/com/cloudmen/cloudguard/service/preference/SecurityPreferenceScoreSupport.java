package com.cloudmen.cloudguard.service.preference;

import java.util.Set;

/**
 * Helpers to ignore disabled security preferences when computing section scores (treat as neutral / omit from averages).
 *
 * @see UserSecurityPreferenceService#getDisabledPreferenceKeys(String)
 */
public final class SecurityPreferenceScoreSupport {

    private SecurityPreferenceScoreSupport() {}

    /**
     * @param disabledKeys   set of {@code section:preferenceKey} strings returned when {@code enabled=false} in DB
     * @param section        settings section (e.g. {@code users-groups})
     * @param preferenceKey  simple key within the section (e.g. {@code 2fa})
     */
    public static boolean preferenceDisabled(Set<String> disabledKeys, String section, String preferenceKey) {
        return disabledKeys != null && disabledKeys.contains(section + ":" + preferenceKey);
    }
}
