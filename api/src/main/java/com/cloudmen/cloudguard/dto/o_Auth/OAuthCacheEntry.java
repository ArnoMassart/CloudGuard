package com.cloudmen.cloudguard.dto.o_Auth;

import java.util.List;

public record OAuthCacheEntry(
        List<RawUserToken> allRawTokens,
        int totalDomainUsers,
        long timestamp
) {
}
