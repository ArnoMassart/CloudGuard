package com.cloudmen.cloudguard.dto.oauth;

import java.util.List;

public record OAuthCacheEntry(
        List<RawUserToken> allRawTokens,
        int totalDomainUsers,
        long timestamp
) {
}
