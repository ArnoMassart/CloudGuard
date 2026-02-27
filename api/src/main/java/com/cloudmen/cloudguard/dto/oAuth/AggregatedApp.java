package com.cloudmen.cloudguard.dto.oAuth;

import java.util.Set;

public record AggregatedApp(
        String clientId,
        String name,
        Set<String> uniqueScopes,
        Set<String> userEmails,
        int userCount
) {
}
