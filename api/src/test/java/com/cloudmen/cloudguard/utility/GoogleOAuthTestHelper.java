package com.cloudmen.cloudguard.utility;

import com.cloudmen.cloudguard.dto.oauth.OAuthCacheEntry;
import com.cloudmen.cloudguard.dto.oauth.RawUserToken;
import com.cloudmen.cloudguard.service.cache.GoogleOAuthCacheService;

import java.util.List;

import static com.cloudmen.cloudguard.utility.GlobalTestHelper.ADMIN;
import static org.mockito.Mockito.*;

public class GoogleOAuthTestHelper {
    public static final String INTERNAL_CLIENT_ID = "1046230096039-0c4fqtecqs3fe7t762cn1922garmr4p5.apps.googleusercontent.com";

    public static RawUserToken createToken(
            String clientId,
            String displayText,
            String userEmail,
            List<String> scopes,
            boolean isNativeApp,
            boolean isAnonymous) {

        // Let op: als RawUserToken geen record is maar een class met setters, pas dit dan even aan naar getters/setters.
        return new RawUserToken(clientId, displayText, userEmail, scopes, isNativeApp, isAnonymous);
    }

    public static void mockCacheEntry(GoogleOAuthCacheService cacheService, List<RawUserToken> tokens, int totalDomainUsers) {
        OAuthCacheEntry mockEntry = mock(OAuthCacheEntry.class);

        lenient().when(mockEntry.allRawTokens()).thenReturn(tokens);
        lenient().when(mockEntry.totalDomainUsers()).thenReturn(totalDomainUsers);

        when(cacheService.getOrFetchOAuthData(ADMIN)).thenReturn(mockEntry);
    }
}
