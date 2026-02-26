package com.cloudmen.cloudguard.dto.users;


import com.google.api.services.admin.directory.model.User;

import java.util.List;
import java.util.Map;

public record UserCacheEntry(
        List<User> allUsers,
        Map<Long, String> roleDictionary,
        Map<String, Long> userRoleAssignments,
        long timestamp
) {
}
