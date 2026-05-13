package com.cloudmen.cloudguard.dto.password;

/**
 * Per–organizational-unit summary for 2-step verification adoption used in password settings and dashboards:
 * enrollment counts versus totals and whether enforcement is effective for that OU path.
 *
 * @param orgUnitPath   Directory path (e.g. {@code /Sales})
 * @param orgUnitName   display name for UI tables
 * @param enforced      {@code true} when policy requires 2SV or enrolled coverage implies enforcement semantics in {@link com.cloudmen.cloudguard.service.PasswordSettingsService}
 * @param enrolledCount users with 2SV enrolled in this OU slice
 * @param totalCount    users attributed to this OU for the same rollup
 */
public record OrgUnit2SvDto(String orgUnitPath, String orgUnitName, boolean enforced, int enrolledCount, int totalCount) {}
