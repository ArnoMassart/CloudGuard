package com.cloudmen.cloudguard.dto.users;

import com.cloudmen.cloudguard.domain.model.UserRole;

import java.util.List;

public record UserDecisionRequestDto(
        String userEmail,
        boolean isAccepted,
        boolean isSuperAdmin,
        String organizationId,
        List<UserRole> roles
) {
}
