package com.cloudmen.cloudguard.dto.password;

import java.util.List;

/**
 * Two-step verification snapshot for password settings: per-OU slices plus tenant-wide totals from Directory.
 *
 * @param byOrgUnit      {@link OrgUnit2SvDto} rows keyed by {@link com.google.api.services.admin.directory.model.User#getOrgUnitPath()}
 * @param totalEnrolled  users with 2SV enrolled flag set
 * @param totalEnforced  users with 2SV enforced flag set
 * @param totalUsers     denominator matching cached Directory pull
 */
public record TwoStepVerificationDto(
        List<OrgUnit2SvDto> byOrgUnit,
        int totalEnrolled,
        int totalEnforced,
        int totalUsers
) {
}
