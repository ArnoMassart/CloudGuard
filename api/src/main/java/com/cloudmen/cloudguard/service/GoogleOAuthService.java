package com.cloudmen.cloudguard.service;

import com.cloudmen.cloudguard.dto.oAuth.*;
import com.cloudmen.cloudguard.service.cache.GoogleOAuthCacheService;
import com.cloudmen.cloudguard.utility.GoogleServiceHelperMethods;
import org.springframework.stereotype.Service;

import java.util.*;

@Service
public class GoogleOAuthService {
    private final GoogleOAuthCacheService oAuthCacheService;

    private static final List<String> INTERNAL_CLIENT_IDS = List.of(
            "1046230096039-0c4fqtecqs3fe7t762cn1922garmr4p5.apps.googleusercontent.com"
    );

    public GoogleOAuthService(GoogleOAuthCacheService oAuthCacheService) {
        this.oAuthCacheService = oAuthCacheService;
    }

    public void forceRefreshCache(String loggedInEmail) {
        oAuthCacheService.forceRefreshCache(loggedInEmail);
    }

    public OAuthPagedResponse getOAuthPaged(String loggedInEmail, String pageToken, int size, String query) {
        OAuthCacheEntry cachedData = oAuthCacheService.getOrFetchOAuthData(loggedInEmail);
        int totalDomainUsers = cachedData.totalDomainUsers();

        List<AggregatedAppBuilder> builders = aggregateTokens(cachedData.allRawTokens());

        if (query != null && !query.trim().isEmpty()) {
            String lowerQuery = query.toLowerCase().trim();
            builders = builders.stream()
                    .filter(app -> app.name != null && app.name.toLowerCase().contains(lowerQuery))
                    .toList();
        }

        builders = builders.stream()
                .sorted((a, b) -> Integer.compare(b.userEmails.size(), a.userEmails.size()))
                .toList();

        int page = GoogleServiceHelperMethods.getPage(pageToken);

        int totalApps = builders.size();
        int startIndex = (page - 1) * size;
        int endIndex = Math.min(startIndex + size, totalApps);

        List<AggregatedAppBuilder> pagedItems = (startIndex >= totalApps)
                ? Collections.emptyList()
                : builders.subList(startIndex, endIndex);

        List<AggregatedAppDto> mappedItems = pagedItems.stream()
                .map(builder -> mapToFrontendDto(builder, totalDomainUsers))
                .toList();

        String nextTokenToReturn = (endIndex < totalApps) ? String.valueOf(page + 1) : null;
        return new OAuthPagedResponse(mappedItems, nextTokenToReturn);
    }

    public OAuthOverviewResponse getOAuthPageOverview(String loggedInEmail) {
        OAuthCacheEntry cachedData = oAuthCacheService.getOrFetchOAuthData(loggedInEmail);
        List<RawUserToken> rawTokens = cachedData.allRawTokens();

        List<AggregatedAppBuilder> apps = aggregateTokens(rawTokens);

        long totalThirdPartyApps = apps.stream().filter(app -> !(app.isAnonymous || INTERNAL_CLIENT_IDS.contains(app.clientId))).count();
        long totalPermissionsGranted = rawTokens.size();

        long totalHighRiskApps = apps.stream()
                .filter(app -> isAppHighRisk(app.allScopes))
                .count();

        int securityScore = 100;
        if (totalThirdPartyApps > 0) {
            double penalty = ((double) totalHighRiskApps / totalThirdPartyApps) * 100;
            securityScore = (int) Math.max(0, 100 - Math.round(penalty));
        }

        return new OAuthOverviewResponse(
                totalThirdPartyApps,
                totalHighRiskApps,
                totalPermissionsGranted,
                securityScore
        );
    }

    private List<AggregatedAppBuilder> aggregateTokens(List<RawUserToken> rawTokens) {
        Map<String, AggregatedAppBuilder> appMap = new HashMap<>();

        for (RawUserToken raw : rawTokens) {
            if (raw.clientId() == null || raw.clientId().isBlank()) continue;

            appMap.computeIfAbsent(raw.clientId(), k -> new AggregatedAppBuilder(raw.clientId(), raw.displayText()))
                    .addGrant(raw);
        }

        return new ArrayList<>(appMap.values());
    }

    private AggregatedAppDto mapToFrontendDto(AggregatedAppBuilder builder, int totalDomainUsers) {
        // Rechten en Risico
        List<DataAccessDto> accessList = builder.allScopes.stream()
                .map(this::translateScopeDetails)
                .distinct()
                .toList();

        int highRiskCount = (int) accessList.stream().filter(DataAccessDto::risk).count();
        boolean isHighRisk = highRiskCount > 0;

        // App Type en Exposure
        String appType = builder.isNative ? "Geïnstalleerde App (Lokaal)" : "Web Applicatie (Cloud)";

        int exposure = 0;
        if (totalDomainUsers > 0) {
            exposure = (int) Math.round(((double) builder.userEmails.size() / totalDomainUsers) * 100);
        }

        // --- NIEUW: Intern of Third-Party logica ---
        // Het is "Intern" als het in jouw eigen lijst staat, of als het een anoniem (ouderwets) intern script is.
        boolean isInternalApp = builder.isAnonymous || INTERNAL_CLIENT_IDS.contains(builder.clientId);
        boolean isThirdParty = !isInternalApp;
        String appSource = isThirdParty ? "Third party" : "Intern";

        return new AggregatedAppDto(
                builder.clientId,
                builder.name != null ? builder.name : "Onbekende App",
                appType,
                appSource,
                isThirdParty,
                builder.isAnonymous,
                isHighRisk,
                builder.userEmails.size(),
                exposure,
                builder.allScopes.size(),
                accessList,
                highRiskCount
        );
    }

    private DataAccessDto translateScopeDetails(String scopeUrl) {
        // Drive
        if (scopeUrl.contains("/auth/drive.readonly")) return new DataAccessDto("Google Drive", "Lezen", false);
        if (scopeUrl.contains("/auth/drive")) return new DataAccessDto("Google Drive", "Volledige toegang", true);

        // Gmail
        if (scopeUrl.contains("/auth/gmail.readonly")) return new DataAccessDto("Gmail", "Lezen", false);
        if (scopeUrl.contains("/auth/gmail.send")) return new DataAccessDto("Gmail", "Berichten verzenden", false);
        if (scopeUrl.contains("/auth/gmail") || scopeUrl.contains("/mail.google.com")) return new DataAccessDto("Gmail", "Volledige toegang", true);

        // Agenda
        if (scopeUrl.contains("/auth/calendar.readonly")) return new DataAccessDto("Google Agenda", "Lezen", false);
        if (scopeUrl.contains("/auth/calendar")) return new DataAccessDto("Google Agenda", "Volledige toegang", true);

        // Contacten
        if (scopeUrl.contains("/auth/contacts.readonly")) return new DataAccessDto("Google Contacten", "Lezen", false);
        if (scopeUrl.contains("/auth/contacts")) return new DataAccessDto("Google Contacten", "Volledige toegang", true);

        // Admin
        if (scopeUrl.contains("/auth/admin.directory")) return new DataAccessDto("Admin Directory", "Beheerdersrechten", true);
        if (scopeUrl.contains("/auth/cloud-platform")) return new DataAccessDto("Cloud Platform", "Cloud beheer", true);

        // Fallback
        String fallbackName = scopeUrl.substring(scopeUrl.lastIndexOf('/') + 1);
        return new DataAccessDto("Overige (" + fallbackName + ")", "Beperkte toegang", false);
    }

    private boolean isAppHighRisk(Set<String> scopes) {
        return scopes.stream().anyMatch(scope ->
                scope.contains("/auth/drive") ||
                        scope.contains("/auth/gmail") ||
                        scope.contains("/auth/admin.directory") ||
                        scope.contains("/auth/cloud-platform")
        );
    }

    // --- INTERNE BUILDER CLASS ---
    private static class AggregatedAppBuilder {
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
}
