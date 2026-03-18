package com.cloudmen.cloudguard.dto.password;

public record OrgUnit2SvDto(
        String orgUnitPath,
        String orgUnitName,
        boolean enforced,
        int enrolledCount,
        int totalCount
) {
}
