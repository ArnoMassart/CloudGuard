package com.cloudmen.cloudguard.dto.apppasswords;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * Single application-specific password returned to the UI (derived from Admin SDK {@link com.google.api.services.admin.directory.model.Asp}).
 */
@AllArgsConstructor
@NoArgsConstructor
@Getter
@Setter
public class AppPasswordDto {
    /** Directory ASP identifier. */
    private Integer codeId;
    /** Human-readable label from Google. */
    private String name;
    /** Creation date formatted as {@code dd-MM-yyyy}. */
    private String creationTime;
    /** Relative “time ago” string from last use timestamp. */
    private String lastTimeUsed;
}
