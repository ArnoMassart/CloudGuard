package com.cloudmen.cloudguard.dto.oauth;

import java.util.List;

public record OAuthPagedResponse(
        List<AggregatedAppDto> apps,
        String nextPageToken,
        int allFilteredApps,
        int allHighRiskApps,
        int allNotHighRiskApps
) {
}
