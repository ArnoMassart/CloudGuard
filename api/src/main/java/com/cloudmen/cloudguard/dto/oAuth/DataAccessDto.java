package com.cloudmen.cloudguard.dto.oAuth;

public record DataAccessDto(
        String name,
        String rights,
        boolean risk
) {
}
