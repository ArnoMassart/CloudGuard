package com.cloudmen.cloudguard.dto.licenses;

public record MfaStats(
        int activeCount,
        int inactiveCount
) {
}
