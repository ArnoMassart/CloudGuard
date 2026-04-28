package com.cloudmen.cloudguard.dto;

/**
 * A Data Transfer Object (DTO) used to store and transfer authentication tokens for the Teamleader API. <p>
 *
 * This record encapsulates both the short-lived access token required to authenticate immediate API requests, and the
 * long-lived refresh token used to obtain new access tokens seamlessly once the current one expires.
 *
 * @param accessToken   the short-lived token used to authenticate and authorize requests to the Teamleader API
 * @param refreshToken  the long-lived token used to securely request a new access token when the current one expires
 */
public record TeamleaderTokens(
        String accessToken,
        String refreshToken
) {}

