package com.cloudmen.cloudguard.dto.oAuth;

import java.util.List;

public record OAuthPagedResponse(
        List<AggregatedAppDto> apps,
        String nextPageToken,
        int allFilteredApps,
        int allHighRiskApps,
        int allNotHighRiskApps
) {
}
