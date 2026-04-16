package com.cloudmen.cloudguard.service.notification;

import com.cloudmen.cloudguard.domain.model.UserRole;

import java.util.List;
import java.util.Map;

/**
 * Maps notification {@code source} (section key from aggregation) to the app role that may see those
 * notifications. {@link UserRole#SUPER_ADMIN} always sees all. Unknown sources are hidden from non–super-admins.
 */
public final class NotificationSourceViewerRoles {

    private static final Map<String, UserRole> SOURCE_TO_VIEWER_ROLE = Map.ofEntries(
            Map.entry("users-groups", UserRole.USERS_GROUPS_VIEWER),
            Map.entry("shared-drives", UserRole.SHARED_DRIVES_VIEWER),
            Map.entry("devices", UserRole.DEVICES_VIEWER),
            Map.entry("mobile-devices", UserRole.DEVICES_VIEWER),
            Map.entry("app-access", UserRole.APP_ACCESS_VIEWER),
            Map.entry("app-passwords", UserRole.APP_PASSWORDS_VIEWER),
            Map.entry("password-settings", UserRole.PASSWORD_SETTINGS_VIEWER),
            Map.entry("domain-dns", UserRole.DOMAIN_DNS_VIEWER));

    private NotificationSourceViewerRoles() {}

    /**
     * Whether the given roles may see notifications for this {@code source} value.
     */
    public static boolean isSourceVisibleToRoles(String source, List<UserRole> roles) {
        if (roles == null || roles.isEmpty()) {
            return false;
        }
        if (roles.contains(UserRole.SUPER_ADMIN)) {
            return true;
        }
        UserRole required = SOURCE_TO_VIEWER_ROLE.get(source);
        if (required == null) {
            return false;
        }
        return roles.contains(required);
    }
}
