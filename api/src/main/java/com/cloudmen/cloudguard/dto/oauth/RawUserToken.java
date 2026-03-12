package com.cloudmen.cloudguard.dto.oauth;

import java.util.List;

public record RawUserToken(
        String userEmail,
        String clientId,
        String displayText,
        List<String> scopes,
        boolean isNativeApp,
        boolean isAnonymous
) {
}
