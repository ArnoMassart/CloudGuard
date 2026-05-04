package com.cloudmen.cloudguard.dto.users;

public record DeniedUser(
        String name,
        String email,
        String accessDeniedReason,
        String accessDeniedAt
) {
}
