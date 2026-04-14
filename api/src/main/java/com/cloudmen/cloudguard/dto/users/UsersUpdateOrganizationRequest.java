package com.cloudmen.cloudguard.dto.users;

public record UsersUpdateOrganizationRequest(
        String userEmail,
        Long orgId
) {
}
