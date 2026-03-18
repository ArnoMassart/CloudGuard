package com.cloudmen.cloudguard.dto.password;

public record SecurityScoreFactorDto(
        String title,
        String description,
        int weightPercent,
        int score,
        int maxScore,
        String severity
) {
}
