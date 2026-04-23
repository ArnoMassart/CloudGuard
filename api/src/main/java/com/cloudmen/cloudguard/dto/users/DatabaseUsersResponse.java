package com.cloudmen.cloudguard.dto.users;

import java.util.List;

public record DatabaseUsersResponse(
        List<UserDto> users,
        String nextPageToken
) {
}
