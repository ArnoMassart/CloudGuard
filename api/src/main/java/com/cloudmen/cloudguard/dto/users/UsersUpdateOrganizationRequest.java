package com.cloudmen.cloudguard.dto.users;

/**
 * A Data Transfer Object (DTO) representing
 * @param userEmail
 * @param orgId
 */
public record UsersUpdateOrganizationRequest(
        String userEmail,
        Long orgId
) {
}
