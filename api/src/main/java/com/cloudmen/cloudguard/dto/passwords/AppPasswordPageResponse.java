package com.cloudmen.cloudguard.dto.passwords;

import java.util.List;

public record AppPasswordPageResponse(
        List<UserAppPasswordsDto> users,
        String nextPageToken
) {
}
