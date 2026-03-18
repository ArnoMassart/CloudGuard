package com.cloudmen.cloudguard.dto.apppasswords;

import java.util.List;

public record AppPasswordPageResponse(
        List<UserAppPasswordsDto> users,
        String nextPageToken
) {
}
