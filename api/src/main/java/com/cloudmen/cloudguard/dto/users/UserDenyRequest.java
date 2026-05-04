package com.cloudmen.cloudguard.dto.users;

public record UserDenyRequest(
        String userEmail,
        String denyReason
) {
}
