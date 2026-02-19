package com.cloudmen.cloudguard.dto;

public record UserOverviewResponse(
            long totalUsers,
            long withoutTwoFactor,
            long adminUsers,
            long securityScore,
            long activeLongNoLoginCount,
            long inactiveRecentLoginCount
){}
