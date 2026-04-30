package com.cloudmen.cloudguard.dto.users;

/**
 * A Data Transfer Object (DTO) used as a request payload to update a user's organizational affiliation. <p>
 *
 * This record encapsulates the necessary parameters required to move or assign a specific user to a different
 * organization within the system.
 *
 * @param userEmail the primary email address of the user whose organizational assignment is being updated
 * @param orgId     the unique database identifier of the new organization to which the user will be assigned
 */
public record UsersUpdateOrganizationRequest(
        String userEmail,
        Long orgId
) {
}
