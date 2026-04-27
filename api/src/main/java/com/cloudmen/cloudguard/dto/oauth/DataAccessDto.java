package com.cloudmen.cloudguard.dto.oauth;

/**
 * A Data Transfer Object (DTO) representing a specific data access permission or scope granted to an application. <p>
 *
 * This record encapsulates the details of an individual OAuth scope, defining what service or data is being accessed,
 * the extent of the permissions granted, and whether those permissions pose a potential security vulnerability.
 *
 * @param name      the name or description of the data access scope or Google service (e.g., "Google Drive", "Gmail")
 * @param rights    the specific level of access or permissions granted (e.g., "Read-only", "Full access")
 * @param risk      {@code true} if this specific data access right or scope is classified as a high security risk
 */
public record DataAccessDto(
        String name,
        String rights,
        boolean risk
) {
}
