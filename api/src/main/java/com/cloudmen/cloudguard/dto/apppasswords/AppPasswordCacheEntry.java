package com.cloudmen.cloudguard.dto.apppasswords;

import java.util.List;

public record AppPasswordCacheEntry(
        List<UserAppPasswordsDto> users,
        int totalUserCount
) {
}