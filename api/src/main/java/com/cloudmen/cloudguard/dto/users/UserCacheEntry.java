package com.cloudmen.cloudguard.dto.users;


import com.google.api.services.admin.directory.model.User;

import java.util.List;
import java.util.Map;

/**
 * A Data Transfer Object (DTO) used to cache a complete snapshot of domain users and their internal application role
 * assignments. <p>
 *
 * This record stores the raw user data from Google Workspace alongside the application's internal Role-Based Access
 * Control (RBAC) mapping. By caching the role dictionary and user assignments together, the system can quickly resolve
 * permissions without redundant database or API lookups.
 *
 * @param allUsers              the complete list of {@link User} entities retrieved from the Google Workspace domain
 * @param roleDictionary        a lookup map associating internal role identifiers (IDs) with their descriptive names
 * @param userRoleAssignments   a mapping of user identifiers (their primary email) to their currently assigned
 *                              internal role ID
 * @param timestamp             the exact time (in milliseconds) when this cache entry was created
 */
public record UserCacheEntry(
        List<User> allUsers,
        Map<Long, String> roleDictionary,
        Map<String, Long> userRoleAssignments,
        long timestamp
) {
}
