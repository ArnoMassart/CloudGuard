package com.cloudmen.cloudguard.service.cache;

import com.cloudmen.cloudguard.dto.oauth.OAuthCacheEntry;
import com.cloudmen.cloudguard.dto.oauth.RawUserToken;
import com.cloudmen.cloudguard.exception.GoogleWorkspaceSyncException;
import com.cloudmen.cloudguard.service.OrganizationService;
import com.cloudmen.cloudguard.service.UserService;
import com.cloudmen.cloudguard.utility.GoogleApiFactory;
import com.cloudmen.cloudguard.utility.GoogleServiceHelperMethods;
import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import com.google.api.services.admin.directory.Directory;
import com.google.api.services.admin.directory.DirectoryScopes;
import com.google.api.services.admin.directory.model.Token;
import com.google.api.services.admin.directory.model.Tokens;
import com.google.api.services.admin.directory.model.User;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.concurrent.TimeUnit;

@Service
public class GoogleOAuthCacheService {
    private static final Logger log = LoggerFactory.getLogger(GoogleOAuthCacheService.class);

    private final GoogleApiFactory googleApiFactory;
    private final GoogleUsersCacheService usersCacheService;

    private final Cache<String, OAuthCacheEntry> cache = Caffeine.newBuilder()
            .expireAfterWrite(1, TimeUnit.HOURS)
            .maximumSize(100)
            .build();
    private final OrganizationService organizationService;
    private final UserService userService;

    public GoogleOAuthCacheService(GoogleApiFactory googleApiFactory, GoogleUsersCacheService usersCacheService, OrganizationService organizationService, UserService userService) {
        this.googleApiFactory = googleApiFactory;
        this.usersCacheService = usersCacheService;
        this.organizationService = organizationService;
        this.userService = userService;
    }

    public void forceRefreshCache(String loggedInEmail) {
        cache.asMap().compute(loggedInEmail, this::fetchFromGoogle);
    }

    public OAuthCacheEntry getOrFetchOAuthData(String loggedInEmail) {
        return cache.get(loggedInEmail, email -> fetchFromGoogle(email, null));
    }

    private OAuthCacheEntry fetchFromGoogle(String loggedInEmail, OAuthCacheEntry fallback) {
        try {
            String adminEmail = GoogleServiceHelperMethods.getAdminEmailForUser(loggedInEmail, userService, organizationService);
            log.info("Ophalen LIVE OAuth data van Google. Gebruiker: {}, Impersonatie via Admin: {}", loggedInEmail, adminEmail);

            Directory directory = googleApiFactory.getDirectoryService(
                    Set.of(DirectoryScopes.ADMIN_DIRECTORY_USER_READONLY, DirectoryScopes.ADMIN_DIRECTORY_USER_SECURITY),
                    adminEmail
            );

            List<User> allUsers = usersCacheService.getOrFetchUsersData(loggedInEmail).allUsers();
            List<RawUserToken> allTokens = fetchAllRawTokens(directory, allUsers);

            return new OAuthCacheEntry(
                    allTokens,
                    allUsers.size(),
                    System.currentTimeMillis()
            );
        } catch (Exception e) {
            if (fallback != null) {
                log.error("Google API faalde! Terugvallen op oude cache: {}", e.getMessage());
                return fallback;
            }
            throw new GoogleWorkspaceSyncException("Fout bij ophalen Google data, en geen cache beschikbaar: " + e.getMessage());
        }
    }

    private List<RawUserToken> fetchAllRawTokens(Directory directory, List<User> users) {
        List<RawUserToken> allTokens = new ArrayList<>();

        for (User user : users) {
            String userEmail = user.getPrimaryEmail();
            try {
                Thread.sleep(50);

                Tokens tokenResponse = directory.tokens().list(userEmail).execute();

                if (tokenResponse.getItems() != null) {
                    for (Token token : tokenResponse.getItems()) {
                        allTokens.add(new RawUserToken(
                                userEmail,
                                token.getClientId(),
                                token.getDisplayText(),
                                token.getScopes(),
                                Boolean.TRUE.equals(token.getNativeApp()),
                                Boolean.TRUE.equals(token.getAnonymous())
                        ));
                    }
                }
            } catch (InterruptedException e) {
                log.warn("OAuth token fetch interrupted for {}", userEmail);
                Thread.currentThread().interrupt();
            } catch (Exception e) {
                log.warn("Kon OAuth tokens voor {} niet ophalen", user.getPrimaryEmail());
            }
        }
        return allTokens;
    }
}


