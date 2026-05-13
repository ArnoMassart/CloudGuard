package com.cloudmen.cloudguard.dto.apppasswords;

import com.cloudmen.cloudguard.dto.password.SecurityScoreBreakdownDto;

/**
 * Aggregated application-specific password metrics for overview APIs and dashboards.
 *
 * @param allowed                whether the module considers app-password reporting enabled for this payload (always {@code true} on success today)
 * @param totalAppPasswords      sum of ASP rows across users who still have passwords
 * @param usersWithAppPasswords  count of users with ≥1 ASP (same as {@link AppPasswordCacheEntry#users()} size)
 * @param securityScore          {@code null} when there are zero workspace users; otherwise {@code 0–100} share without ASPs
 * @param securityScoreBreakdown translated factor row for the score card; {@code null} when {@code securityScore} is {@code null}
 */
public record AppPasswordOverviewResponse(
        boolean allowed,
        int totalAppPasswords,
        int usersWithAppPasswords,
        Integer securityScore,
        SecurityScoreBreakdownDto securityScoreBreakdown) {}
