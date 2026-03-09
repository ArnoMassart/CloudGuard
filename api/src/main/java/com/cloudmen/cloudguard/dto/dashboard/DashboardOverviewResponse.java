package com.cloudmen.cloudguard.dto.dashboard;

public record DashboardOverviewResponse(
        int totalNotifications,
        int criticalNotifications
) {
}
