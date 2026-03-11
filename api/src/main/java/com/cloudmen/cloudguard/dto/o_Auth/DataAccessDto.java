package com.cloudmen.cloudguard.dto.o_Auth;

public record DataAccessDto(
        String name,
        String rights,
        boolean risk
) {
}
