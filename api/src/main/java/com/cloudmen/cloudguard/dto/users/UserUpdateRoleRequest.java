package com.cloudmen.cloudguard.dto.users;

import com.cloudmen.cloudguard.domain.model.UserRole;

import java.util.List;

/**
 * A Data Transfer Object (DTO) used as a request payload to update the roles assigned to a specific user. <p>
 *
 * This record encapsulates the necessary data to modify a user's access rights and permissions within the system's
 * Role-Based Access Control (RBAC) framework.
 *
 * @param userEmail the primary email address of the user whose roles are being updated
 * @param roles     the complete list of {@link UserRole} enumerations to be assigned to the user, typically overwriting
 *                  their previous access rights
 */
public record UserUpdateRoleRequest(
        String userEmail,
        List<UserRole> roles
) {
}
