package com.cloudmen.cloudguard.service.oauth;

import com.cloudmen.cloudguard.dto.oauth.RawUserToken;

import java.util.HashSet;
import java.util.Set;

/**
 * Internal helper class used to aggregate multiple raw OAuth tokens (grants) belonging to the same Client ID into a
 * single application representation.
 */
public class AggregatedAppBuilder {
    final String clientId;
    final String name;
    final Set<String> userEmails = new HashSet<>();
    final Set<String> allScopes = new HashSet<>();
    boolean isNative = false;
    boolean isAnonymous = false;

    AggregatedAppBuilder(String clientId, String name) {
        this.clientId = clientId;
        this.name = name;
    }

    void addGrant(RawUserToken token) {
        if (token.userEmail() != null) userEmails.add(token.userEmail());
        if (token.scopes() != null) allScopes.addAll(token.scopes());
        if (token.isNativeApp()) this.isNative = true;
        if (token.isAnonymous()) this.isAnonymous = true;
    }
}
