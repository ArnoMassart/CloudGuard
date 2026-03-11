package com.cloudmen.cloudguard.dto.dashboard;

public record DashboardPageResponse(
        DashboardScores scores,
        int overallScore,
        String lastUpdated
        ) {
}
