package com.cloudmen.cloudguard.dto.users;

import java.util.List;

/**
 * A Data Transfer Object (DTO) containing detailed organizational and security information for a user. <p>
 *
 * This record encapsulates a user's identity details, their activity status, and comprehensive security
 * compliance metrics. It is primarily used to display user lists alongside their corresponding security health within
 * an organization.
 */
public record UserOrgDetail(
        String fullName,
        String email,
        String role,
        boolean isActive,
        String lastLogin,
        boolean isTwoFactorEnabled,
        boolean isSecurityConform,
//      Codes: NO_2FA, ACTIVITY_STALE, ACTIVITY_INACTIVE_RECENT — used to mask "Niet conform" when matching prefs are off.
        List<String> securityViolationCodes
) { }
