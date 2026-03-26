package com.cloudmen.cloudguard.dto.users;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;

import java.util.List;

@AllArgsConstructor
@Getter
@Setter
public class UserOrgDetail {
    private String fullName;
    private String email;
    private String role;
    private boolean isActive;
    private String lastLogin;
    private boolean isTwoFactorEnabled;
    private boolean isSecurityConform;
    /** Codes: NO_2FA, ACTIVITY_STALE, ACTIVITY_INACTIVE_RECENT — used to mask "Niet conform" when matching prefs are off. */
    private List<String> securityViolationCodes;
}
