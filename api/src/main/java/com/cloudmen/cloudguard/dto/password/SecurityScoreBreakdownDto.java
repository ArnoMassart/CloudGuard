package com.cloudmen.cloudguard.dto.password;

import java.util.List;

public record SecurityScoreBreakdownDto(
        int totalScore,
        String status,
        List<SecurityScoreFactorDto> factors
) {
}
