package com.cloudmen.cloudguard.dto.dashboard;

public record DashboardScores(
        int usersScore,
        int groupsScore,
        int drivesScore,
        int devicesScore,
        int appAccessScore,
        int appPasswordsScore,
        int passwordSettingsScore,
        int dnsScore
) {
}
