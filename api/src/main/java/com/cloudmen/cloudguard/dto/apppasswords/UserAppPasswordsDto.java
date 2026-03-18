package com.cloudmen.cloudguard.dto.apppasswords;

import java.util.List;

public record UserAppPasswordsDto(
        String id,
        String name,
        String email,
        String role,
        boolean tsv,
        List<AppPasswordDto> passwords
) {
}
