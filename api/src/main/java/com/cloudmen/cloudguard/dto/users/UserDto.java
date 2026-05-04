package com.cloudmen.cloudguard.dto.users;

import com.cloudmen.cloudguard.domain.model.UserRole;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.time.LocalDateTime;
import java.util.List;

/**
 * A Data Transfer Object (DTO) representing a user within the application <p>
 *
 * This record encapsulates the user's core identity details, their assigned internal roles for access control,
 * and their organizational affiliation. It also tracks pending requests for role elevations or organization joins.
 *
 * @param email                 the primary email address of the user, used as their unique login identifier
 * @param firstName             the given name of the user
 * @param lastName              the family name or surname of the user
 * @param pictureUrl            the URL pointing to the user's profile picture or avatar
 * @param roles                 a list of {@link UserRole} enumerations assigned to the user, defining their
 *                              access rights
 * @param createdAt             the local date and time when this user record was created or registered in the system
 * @param roleRequested         {@code true} if the user has a pending request to be granted a new or elevated role
 * @param organizationRequested {@code true} if the user has a pending request to join or be assigned to an
 *                              organization
 * @param accessRequested       {@code true} if the user has a pending request to join
 * @param accessAccepted        {@code true} if the user has been accepted to CloudGuard
 * @param accessDenied          {@code true} if the user has been denied from CloudGuard
 * @param organizationId        the unique database identifier of the organization to which the user belongs
 * @param organizationName      the display name of the organization to which the user is affiliated
 */
public record UserDto(
        String email,
        String firstName,
        String lastName,
        String pictureUrl,
        List<UserRole> roles,
        LocalDateTime createdAt,
        Boolean isActive,
        Boolean roleRequested,
        Boolean organizationRequested,
        Boolean accessRequested,
        Boolean accessAccepted,
        Boolean accessDenied,
        Long organizationId,
        String organizationName,
        @JsonProperty("isCloudmenStaff") Boolean isCloudmenStaff
        ) {

}
