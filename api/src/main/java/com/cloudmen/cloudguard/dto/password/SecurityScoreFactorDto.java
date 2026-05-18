package com.cloudmen.cloudguard.dto.password;

/**
 * Single contributor row inside {@link SecurityScoreBreakdownDto}. Built via
 * {@link com.cloudmen.cloudguard.utility.GoogleServiceHelperMethods#securityScoreFactorForDetail}.
 *
 * @param title       short factor label (sometimes localized upstream)
 * @param description longer explanation for tooltips
 * @param score       realized points for this slice
 * @param maxScore    nominal ceiling when the factor applies ({@code 0} max hides bar segments when {@code muted} workflow applies)
 * @param severity    coarse styling bucket ({@code success}, {@code warning}, …)
 * @param muted       {@code true} when excluded from scoring due to user preference muting (UI may grey out)
 */
public record SecurityScoreFactorDto(
        String title,
        String description,
        int score,
        int maxScore,
        String severity,
        boolean muted
) {
    /**
     * Convenience constructor for factors without preference muting.
     *
     * @see SecurityScoreFactorDto
     */
    public SecurityScoreFactorDto(String title, String description, int score, int maxScore, String severity) {
        this(title, description, score, maxScore, severity, false);
    }
}
