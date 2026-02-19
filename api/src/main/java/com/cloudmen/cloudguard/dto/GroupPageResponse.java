package com.cloudmen.cloudguard.dto;

import java.util.List;

public record GroupPageResponse(
        List<GroupOrgDetail> groups,
        String nextPageToken
) {}
