package com.cloudmen.cloudguard.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;

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
}
