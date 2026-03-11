package com.cloudmen.cloudguard.dto.oauth;

public record DataAccessDto(
        String name,
        String rights,
        boolean risk
) {
}
