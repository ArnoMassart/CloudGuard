package com.cloudmen.cloudguard.dto.groups;

import java.util.List;

public record GroupPageResponse(
        List<GroupOrgDetail> groups,
        String nextPageToken
) {}
