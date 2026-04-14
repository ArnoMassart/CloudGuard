package com.cloudmen.cloudguard.dto.users;

import com.cloudmen.cloudguard.domain.model.UserRole;

import java.util.List;

public record UserUpdateRoleRequest(
        String userEmail,
        List<UserRole> roles
) {
}
