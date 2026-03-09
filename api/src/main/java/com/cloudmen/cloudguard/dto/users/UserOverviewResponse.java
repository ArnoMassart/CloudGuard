package com.cloudmen.cloudguard.dto.users;

public record UserOverviewResponse(
            int totalUsers,
            int withoutTwoFactor,
            int adminUsers,
            int securityScore,
            int activeLongNoLoginCount,
            int inactiveRecentLoginCount
){}
