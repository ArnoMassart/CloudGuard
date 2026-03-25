package com.cloudmen.cloudguard.service.preference;

import java.util.Map;
import java.util.Set;

/**
 * Maps (source, notificationType) to (section, preferenceKey).
 * When a user disables a preference, all notifications with matching source:notificationType are hidden.
 */
public final class PreferenceToNotificationMapping {

    private PreferenceToNotificationMapping() {}

    /**
     * Maps "source:notificationType" -> "section:preferenceKey"
     */
    private static final Map<String, String> NOTIFICATION_TO_PREFERENCE = Map.ofEntries(
            // domain-dns
            Map.entry("domain-dns:dns-critical", "domain-dns:dnsCritical"),
            Map.entry("domain-dns:dns-attention", "domain-dns:dnsAttention"),
            // users-groups
            Map.entry("users-groups:user-control", "users-groups:2fa"),
            Map.entry("users-groups:user-activity", "users-groups:activity"),
            Map.entry("users-groups:group-external", "users-groups:groupExternal"),
            // shared-drives
            Map.entry("shared-drives:drive-orphan", "shared-drives:orphan"),
            Map.entry("shared-drives:drive-external", "shared-drives:external"),
            Map.entry("shared-drives:drive-outside-domain", "shared-drives:outsideDomain"),
            Map.entry("shared-drives:drive-non-member-access", "shared-drives:nonMemberAccess"),
            // mobile-devices
            Map.entry("mobile-devices:device-lockscreen", "mobile-devices:lockscreen"),
            Map.entry("mobile-devices:device-encryption", "mobile-devices:encryption"),
            Map.entry("mobile-devices:device-os", "mobile-devices:osVersion"),
            Map.entry("mobile-devices:device-integrity", "mobile-devices:integrity"),
            // app-access
            Map.entry("app-access:oauth-high-risk", "app-access:highRisk"),
            // app-passwords
            Map.entry("app-passwords:app-password", "app-passwords:appPassword"),
            // password-settings
            Map.entry("password-settings:password-2sv-not-enforced", "password-settings:2sv"),
            Map.entry("password-settings:password-weak-length", "password-settings:length"),
            Map.entry("password-settings:password-strong-not-required", "password-settings:strongPassword"),
            Map.entry("password-settings:password-never-expires", "password-settings:expiration"),
            Map.entry("password-settings:password-admins-no-security-keys", "password-settings:adminsSecurityKeys")
    );

    /**
     * Returns true if the notification (source, notificationType) should be hidden
     * when the user has disabled the corresponding preference.
     */
    public static String getPreferenceKey(String source, String notificationType) {
        return NOTIFICATION_TO_PREFERENCE.get(source + ":" + notificationType);
    }

    /**
     * Check if a notification is disabled by user preferences.
     * disabledPreferenceKeys should be the set of "section:preferenceKey" where enabled=false.
     */
    public static boolean isDisabledByPreference(String source, String notificationType, Set<String> disabledPreferenceKeys) {
        String prefKey = getPreferenceKey(source, notificationType);
        return prefKey != null && disabledPreferenceKeys.contains(prefKey);
    }

    /**
     * All (section, preferenceKey) pairs that can be toggled.
     * Used for defaults and settings UI.
     */
    public static Set<String> getAllPreferenceKeys() {
        return Set.copyOf(NOTIFICATION_TO_PREFERENCE.values());
    }
}
