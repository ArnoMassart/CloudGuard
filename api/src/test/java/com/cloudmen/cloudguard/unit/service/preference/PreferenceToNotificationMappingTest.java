package com.cloudmen.cloudguard.unit.service.preference;

import com.cloudmen.cloudguard.service.preference.PreferenceToNotificationMapping;
import com.cloudmen.cloudguard.service.preference.UserSecurityPreferenceService;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Tests the static map from API notification identifiers ({@code source:notificationType})
 * to persisted preference keys ({@code section:preferenceKey}). This is what the UI toggles
 * and what {@link UserSecurityPreferenceService#isNotificationHiddenByPreference} consults.
 */
class PreferenceToNotificationMappingTest {

    @Test
    void getPreferenceKey_resolvesKnownUsersGroupsMapping() {
        Assertions.assertEquals("users-groups:2fa", PreferenceToNotificationMapping.getPreferenceKey("users-groups", "user-control"));
    }

    @Test
    void getPreferenceKey_resolvesKnownPasswordSettingsMapping() {
        assertEquals(
                "password-settings:adminsSecurityKeys",
                PreferenceToNotificationMapping.getPreferenceKey("password-settings", "password-admins-no-security-keys"));
    }

    @Test
    void getPreferenceKey_unknownSourceOrType_returnsNull() {
        assertNull(PreferenceToNotificationMapping.getPreferenceKey("unknown-source", "x"));
        assertNull(PreferenceToNotificationMapping.getPreferenceKey("password-settings", "not-a-real-type"));
    }

    @Test
    void isDisabledByPreference_trueWhenMappedKeyIsInDisabledSet() {
        Set<String> disabled = Set.of("password-settings:2sv");
        assertTrue(PreferenceToNotificationMapping.isDisabledByPreference(
                "password-settings", "password-2sv-not-enforced", disabled));
    }

    @Test
    void isDisabledByPreference_falseWhenNotificationNotMapped() {
        assertFalse(PreferenceToNotificationMapping.isDisabledByPreference(
                "password-settings", "unknown-notification", Set.of("password-settings:2sv")));
    }

    @Test
    void isDisabledByPreference_falseWhenMappedButNotInDisabledSet() {
        assertFalse(PreferenceToNotificationMapping.isDisabledByPreference(
                "password-settings", "password-2sv-not-enforced", Set.of("password-settings:length")));
    }

    @Test
    void getAllPreferenceKeys_matchesNotificationTableSize() {
        Set<String> keys = PreferenceToNotificationMapping.getAllPreferenceKeys();
        assertNotNull(keys);
        assertEquals(18, keys.size());
        assertTrue(keys.contains("users-groups:2fa"));
        assertTrue(keys.contains("password-settings:strongPassword"));
        assertTrue(keys.contains("mobile-devices:lockscreen"));
    }
}
