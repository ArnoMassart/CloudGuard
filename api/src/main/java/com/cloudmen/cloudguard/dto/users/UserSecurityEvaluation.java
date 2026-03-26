package com.cloudmen.cloudguard.dto.users;

import java.util.List;

/**
 * Per-user security row: overall conform flag (unchanged rules) plus machine codes for viewer preference masking.
 */
public record UserSecurityEvaluation(boolean conform, List<String> violationCodes) {}
