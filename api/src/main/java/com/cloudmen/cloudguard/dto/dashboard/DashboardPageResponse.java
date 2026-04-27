package com.cloudmen.cloudguard.dto.dashboard;

/**
 * A Data Transfer Object (DTO) representing the primary data payload for the dashboard page. <p>
 *
 * This record encapsulates the detailed category scores, and aggregated overall score, and a timestamp indicating
 * when those metrics were last calculated. It provides the frontend with a comprehensive view of the organization's
 * current security posture.
 *
 * @param scores        the detailed breakdown of various security or system-specific scores
 * @param overallScore  the aggregated numerical score representing the overall organizational health or status
 * @param lastUpdated   a formatted string indicating the exact time these metrics were last refreshed or generated
 */
public record DashboardPageResponse(
        DashboardScores scores,
        int overallScore,
        String lastUpdated
        ) {
}
