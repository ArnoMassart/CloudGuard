package com.cloudmen.cloudguard.service.preference;

import java.util.Map;
import java.util.Set;

/**
 * Maps ({@code source}, {@code notificationType}) to ({@code section}, {@code preferenceKey}) for mute toggles.
 * When a user disables a preference (stored as {@code enabled=false}), matching notifications are filtered out.
 *
 * @see UserSecurityPreferenceService#getDisabledPreferenceKeys(String)
 */
public final class PreferenceToNotificationMapping {

    private PreferenceToNotificationMapping() {}

    /**
     * Maps {@code source:notificationType} → {@code section:preferenceKey} for known notifications.
     */
    private static final Map<String, String> NOTIFICATION_TO_PREFERENCE = Map.ofEntries(
            // users-groups
            Map.entry("users-groups:user-control", "users-groups:2fa"),
            Map.entry("users-groups:user-activity", "users-groups:activity"),
            Map.entry("users-groups:group-external", "users-groups:groupExternal"),
            // shared-drives
            Map.entry("shared-drives:drive-orphan", "shared-drives:orphan"),
            Map.entry("shared-drives:drive-external", "shared-drives:external"),
            Map.entry("shared-drives:drive-outside-domain", "shared-drives:outsideDomain"),
            Map.entry("shared-drives:drive-non-member-access", "shared-drives:nonMemberAccess"),
            // devices
            Map.entry("devices:device-lockscreen", "devices:lockscreen"),
            Map.entry("devices:device-encryption", "devices:encryption"),
            Map.entry("devices:device-os", "devices:osVersion"),
            Map.entry("devices:device-integrity", "devices:integrity"),
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
     * @return composite {@code section:preferenceKey} when this notification can be muted; otherwise {@code null}
     */
    public static String getPreferenceKey(String source, String notificationType) {
        return NOTIFICATION_TO_PREFERENCE.get(source + ":" + notificationType);
    }

    /**
     * {@code true} when {@code disabledPreferenceKeys} contains the mapped preference for this notification.
     *
     * @param disabledPreferenceKeys {@code section:preferenceKey} entries with {@code enabled=false}
     */
    public static boolean isDisabledByPreference(String source, String notificationType, Set<String> disabledPreferenceKeys) {
        String prefKey = getPreferenceKey(source, notificationType);
        return prefKey != null && disabledPreferenceKeys.contains(prefKey);
    }

    /**
     * All {@code section:preferenceKey} pairs exposed as boolean toggles in settings / defaults.
     */
    public static Set<String> getAllPreferenceKeys() {
        return Set.copyOf(NOTIFICATION_TO_PREFERENCE.values());
    }
}
