package com.cloudmen.cloudguard.dto.passwords;

import java.util.List;

public record UserAppPasswordsDto(
        String name,
        String email,
        String role,
        boolean tsv,
        List<AppPasswordDto> passwords
) {
}
