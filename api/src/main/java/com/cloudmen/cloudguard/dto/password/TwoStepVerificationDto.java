package com.cloudmen.cloudguard.dto.password;

import java.util.List;

public record TwoStepVerificationDto(
        List<OrgUnit2SvDto> byOrgUnit,
        int totalEnrolled,
        int totalEnforced,
        int totalUsers
) {
}
