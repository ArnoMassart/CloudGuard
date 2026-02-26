package com.cloudmen.cloudguard.dto.users;

import java.util.List;

public record UserPageResponse(
        List<UserOrgDetail> users,
        String nextPageToken
) {}
