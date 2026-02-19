package com.cloudmen.cloudguard.dto;

import java.util.List;

public record UserPageResponse(
        List<UserOrgDetail> users,
        String nextPageToken
) {}
