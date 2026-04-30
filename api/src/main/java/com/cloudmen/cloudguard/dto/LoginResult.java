package com.cloudmen.cloudguard.dto;

import com.cloudmen.cloudguard.dto.users.UserDto;
import org.springframework.http.ResponseCookie;

/**
 * A Data Transfer Object (DTO) encapsulating the outcome of a successful user authentication process. <p>
 *
 * This record bundles the HTTP response cookie (typically containing a session identifier or secure token) alongside
 * the authenticated user's profile details. It allows the system to simultaneously return the necessary authorization
 * artifacts and the user context to the frontend.
 *
 * @param cookie    the HTTP response cookie generated upon successful login, usually containing the authentication
 *                  token
 * @param userDto   the {@link UserDto} object containing the profile details and permissions of the authenticated user
 */
public record LoginResult(
        ResponseCookie cookie,
        UserDto userDto
) {}
