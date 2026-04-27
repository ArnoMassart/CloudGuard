package com.cloudmen.cloudguard.dto.oauth;

import java.util.List;

/**
 * A Data Transfer Object (DTO) used to cache the raw OAuth token data for an organization. <p>
 *
 * This record holds the complete, unaggregated list of user tokens retrieved from Google Workspace, along with the
 * total user count for exposure calculations and a timestamp to manage cache expiration.
 *
 * @param allRawTokens      the complete list of raw OAuth tokens retrieved across all users in the domain
 * @param totalDomainUsers  the total number of users within the organization's domain, used to calculate application
 *                          exposure percentages
 * @param timestamp         the exact time (in milliseconds) when this cache entry was created
 */
public record OAuthCacheEntry(
        List<RawUserToken> allRawTokens,
        int totalDomainUsers,
        long timestamp
) {
}
