package com.cloudmen.cloudguard.dto.password;

public record SecurityScoreFactorDto(
        String title,
        String description,
        int score,
        int maxScore,
        String severity,
        boolean muted
) {
    /** @param muted when true, this factor is excluded from the score because the user muted the matching security preference (UI may style as inactive). */
    public SecurityScoreFactorDto(String title, String description, int score, int maxScore, String severity) {
        this(title, description, score, maxScore, severity, false);
    }
}
