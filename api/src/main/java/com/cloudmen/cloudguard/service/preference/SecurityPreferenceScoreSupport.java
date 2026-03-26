package com.cloudmen.cloudguard.service.preference;

import java.util.Set;

/**
 * Helpers to ignore disabled security preferences when computing section scores (treat as neutral / omit from averages).
 */
public final class SecurityPreferenceScoreSupport {

    private SecurityPreferenceScoreSupport() {}

    public static boolean preferenceDisabled(Set<String> disabledKeys, String section, String preferenceKey) {
        return disabledKeys != null && disabledKeys.contains(section + ":" + preferenceKey);
    }
}
