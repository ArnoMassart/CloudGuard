package com.cloudmen.cloudguard.dto.adminsecuritykeys;

import java.util.List;

public record AdminSecurityKeysResponse(
        List<AdminWithSecurityKeyDto> admins,
        int totalAdmins,
        String errorMessage
) {
    public AdminSecurityKeysResponse(List<AdminWithSecurityKeyDto> admins, int totalAdmins) {
        this(admins, totalAdmins, null);
    }

    public AdminSecurityKeysResponse(List<AdminWithSecurityKeyDto> admins, String errorMessage) {
        this(admins, admins != null ? admins.size() : 0, errorMessage);
    }
}
