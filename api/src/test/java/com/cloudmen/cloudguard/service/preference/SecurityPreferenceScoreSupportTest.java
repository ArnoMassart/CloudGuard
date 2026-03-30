package com.cloudmen.cloudguard.service.preference;

import org.junit.jupiter.api.Test;

import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class SecurityPreferenceScoreSupportTest {

    @Test
    void preferenceDisabled_falseForNullSet() {
        assertFalse(SecurityPreferenceScoreSupport.preferenceDisabled(null, "password-settings", "2sv"));
    }

    @Test
    void preferenceDisabled_falseWhenKeyMissing() {
        assertFalse(SecurityPreferenceScoreSupport.preferenceDisabled(
                Set.of("password-settings:length"), "password-settings", "2sv"));
    }

    @Test
    void preferenceDisabled_trueWhenSectionKeyPresent() {
        assertTrue(SecurityPreferenceScoreSupport.preferenceDisabled(
                Set.of("password-settings:2sv", "mobile-devices:lockscreen"),
                "password-settings", "2sv"));
    }
}
