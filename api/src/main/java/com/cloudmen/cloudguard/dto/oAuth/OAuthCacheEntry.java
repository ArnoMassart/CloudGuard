package com.cloudmen.cloudguard.dto.oAuth;

import java.util.List;

public record OAuthCacheEntry(
        List<RawUserToken> allRawTokens,
        int totalDomainUsers,
        long timestamp
) {
}
