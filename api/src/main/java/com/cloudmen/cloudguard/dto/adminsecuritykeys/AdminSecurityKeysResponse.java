package com.cloudmen.cloudguard.dto.adminsecuritykeys;

import java.util.List;

public record AdminSecurityKeysResponse(
        List<AdminWithSecurityKeyDto> admins,
        String errorMessage
) {
    public AdminSecurityKeysResponse(List<AdminWithSecurityKeyDto> admins) {
        this(admins, null);
    }
}
