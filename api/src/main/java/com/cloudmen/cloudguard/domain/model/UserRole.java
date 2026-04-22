package com.cloudmen.cloudguard.domain.model;

/**
 * Enum representing the various roles and permissions a user can hold within the system. <p>
 *
 * These roles are used to enforce role-based access control (RBAC), determining which modules, features, and data
 * a user is authorized to view or interact with.
 */
public enum UserRole {
    /**
     * Represents a user who has not yet been assigned any specific role or permissions. <p>
     *
     * This is typically the default state for newly created or registered users.
     */
    UNASSIGNED,

    /**
     * Represents a system administrator with full access to all features, settings, and modules across the application.
     */
    SUPER_ADMIN,

    /**
     * Grants permission to view user and group data within the organization.
     */
    USERS_GROUPS_VIEWER,

    /**
     * Grants permission to view the organizational unit (OU) structure and details.
     */
    ORG_UNITS_VIEWER,

    /**
     * Grants permission to view details, settings, and metrics regarding shared drives.
     */
    SHARED_DRIVES_VIEWER,

    /**
     * Grants permission to view registered devices, their statuses, and compliance information.
     */
    DEVICES_VIEWER,

    /**
     * Grants permission to view internal and third-party applications access (e.g., OAuth integrations)
     * and associated risk levels.
     */
    APP_ACCESS_VIEWER,

    /**
     * Grants permission to view the usage and configuration of application-specific passwords.
     */
    APP_PASSWORDS_VIEWER,

    /**
     * Grants permission to view password policies, 2-Step Verification (2SV) enforcement,
     * and other related password settings.
     */
    PASSWORD_SETTINGS_VIEWER,

    /**
     * Grants permission to view domain configurations and DNS records (e.g., SPF, DKIM, DMARC).
     */
    DOMAIN_DNS_VIEWER,

    /**
     * Grants permission to view Google Workspace or system license allocations and usage.
     */
    LICENSES_VIEWER,

    /**
     * Grants permission to view the security preferences and notification settings.
     */
    SECURITY_PREFERENCES_VIEWER
}
