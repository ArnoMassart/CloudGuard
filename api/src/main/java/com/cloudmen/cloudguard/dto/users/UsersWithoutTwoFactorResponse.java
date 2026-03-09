package com.cloudmen.cloudguard.dto.users;

import java.util.List;

public record UsersWithoutTwoFactorResponse(List<UserSummary> users) {
    public record UserSummary(String fullName, String email) {}
}
