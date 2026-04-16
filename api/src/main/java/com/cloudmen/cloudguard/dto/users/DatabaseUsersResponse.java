package com.cloudmen.cloudguard.dto.users;

import com.cloudmen.cloudguard.domain.model.User;

import java.util.List;

public record DatabaseUsersResponse(
        List<UserDto> users,
        String nextPageToken
) {
}
