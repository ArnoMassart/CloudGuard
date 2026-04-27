package com.cloudmen.cloudguard.dto.dashboard;

/**
 * A Data Transfer Object (DTO) representing a high-level overview of dashboard metrics. <p>
 *
 * This record encapsulates aggregated notification statistics, providing a quick snapshot of the system's current
 * alert status. It includes the total volume of notifications and the specific number of critical alerts requiring
 * immediate attention.
 *
 * @param totalNotifications    the total number of active notifications currently logged in the system
 * @param criticalNotifications the subset of notifications categorized as critical, demanding urgent action
 */
public record DashboardOverviewResponse(
        int totalNotifications,
        int criticalNotifications
) {
}
