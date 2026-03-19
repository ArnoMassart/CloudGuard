package com.cloudmen.cloudguard.dto.password;

public record SecurityScoreFactorDto(
        String title,
        String description,
        int score,
        int maxScore,
        String severity
) {
}
