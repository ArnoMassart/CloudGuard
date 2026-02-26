package com.cloudmen.cloudguard.dto.users;

public record UserOverviewResponse(
            long totalUsers,
            long withoutTwoFactor,
            long adminUsers,
            long securityScore,
            long activeLongNoLoginCount,
            long inactiveRecentLoginCount
){}
