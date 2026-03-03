package com.cloudmen.cloudguard.dto.passwords;

import java.util.List;

public record AppPasswordCacheEntry(
        List<UserAppPasswordsDto> users,
        int totalUserCount
) {
}