package com.cloudmen.cloudguard.service;

import com.cloudmen.cloudguard.dto.oauth.*;
import com.cloudmen.cloudguard.service.cache.GoogleOAuthCacheService;
import com.cloudmen.cloudguard.utility.GoogleServiceHelperMethods;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.stream.Collectors;

@Service
public class GoogleOAuthService {
    private final GoogleOAuthCacheService oAuthCacheService;

    private static final List<String> INTERNAL_CLIENT_IDS = List.of(
            "1046230096039-0c4fqtecqs3fe7t762cn1922garmr4p5.apps.googleusercontent.com"
    );

    private static final String FULL_ACCESS_READ_WRITE_TEXT = "Volledige toegang (Lezen/Schrijven)";

    private record ScopeMapping(String keyword, ScopeCategory category, String description, boolean isCritical) {}

    private static final List<ScopeMapping> SCOPE_MAPPINGS = List.of(
            // --- 1. IDENTITEIT & INLOGGEN (SSO) ---
            new ScopeMapping("openid", ScopeCategory.SIGN_IN, "Basis profielgegevens", false),
            new ScopeMapping("userinfo.profile", ScopeCategory.SIGN_IN, "Basis profielgegevens", false),
            new ScopeMapping("userinfo.email", ScopeCategory.SIGN_IN, "E-mailadres inzien", false),

            // --- 2. GMAIL ---
            new ScopeMapping("/auth/gmail.readonly", ScopeCategory.GMAIL, "E-mails lezen", false),
            new ScopeMapping("/auth/gmail.send", ScopeCategory.GMAIL, "Berichten verzenden", false),
            new ScopeMapping("/auth/gmail.compose", ScopeCategory.GMAIL, "Concepten aanmaken", false),
            new ScopeMapping("/auth/gmail.modify", ScopeCategory.GMAIL, "E-mails wijzigen/verwijderen", true),
            new ScopeMapping("/auth/gmail.settings.basic", ScopeCategory.GMAIL_SETTINGS, "Mailbox instellingen inzien", false),
            new ScopeMapping("/auth/gmail", ScopeCategory.GMAIL, FULL_ACCESS_READ_WRITE_TEXT, true),
            new ScopeMapping("/mail.google.com", ScopeCategory.GMAIL, FULL_ACCESS_READ_WRITE_TEXT, true),

            // --- 3. GOOGLE DRIVE ---
            new ScopeMapping("/auth/drive.readonly", ScopeCategory.DRIVE, "Alle bestanden lezen", false),
            new ScopeMapping("/auth/drive.file", ScopeCategory.DRIVE, "Alleen eigen gemaakte bestanden inzien", false),
            new ScopeMapping("/auth/drive.appdata", ScopeCategory.DRIVE, "Verborgen app-data opslaan", false),
            new ScopeMapping("/auth/drive.metadata", ScopeCategory.DRIVE, "Bestandsnamen & mappenstructuur inzien", false),
            new ScopeMapping("/auth/drive", ScopeCategory.DRIVE, FULL_ACCESS_READ_WRITE_TEXT, true),

            // --- 4. OFFICE APPS (Docs, Sheets, Forms) ---
            new ScopeMapping("/auth/spreadsheets.readonly", ScopeCategory.SHEETS, "Spreadsheets inzien", false),
            new ScopeMapping("/auth/spreadsheets", ScopeCategory.SHEETS, "Spreadsheets bewerken", true),
            new ScopeMapping("/auth/documents.readonly", ScopeCategory.DOCS, "Documenten inzien", false),
            new ScopeMapping("/auth/documents", ScopeCategory.DOCS, "Documenten bewerken", true),
            new ScopeMapping("/auth/forms.responses.readonly", ScopeCategory.FORMS, "Ingevulde antwoorden inzien", false),
            new ScopeMapping("/auth/forms", ScopeCategory.FORMS, "Formulieren beheren", true),

            // --- 5. AGENDA & CONTACTEN ---
            new ScopeMapping("/auth/calendar.readonly", ScopeCategory.CALENDAR, "Agenda lezen", false),
            new ScopeMapping("/auth/calendar.events", ScopeCategory.CALENDAR, "Afspraken beheren", false),
            new ScopeMapping("/auth/calendar", ScopeCategory.CALENDAR, "Volledige agenda-toegang", true),
            new ScopeMapping("/auth/contacts.readonly", ScopeCategory.CONTACTS, "Contactpersonen inzien", false),
            new ScopeMapping("/auth/contacts.other.readonly", ScopeCategory.CONTACTS, "Gedeelde contacten inzien", false),
            new ScopeMapping("/auth/contacts", ScopeCategory.CONTACTS, "Contactpersonen beheren", true),

            // --- 6. BEHEERDERS & DIRECTORY (KRITIEK!) ---
            new ScopeMapping("/auth/admin.directory.user.readonly", ScopeCategory.ORG_STRUCTURE, "Gebruikerslijst inzien", false),
            new ScopeMapping("/auth/admin.directory.user", ScopeCategory.ADMIN_DIRECTORY, "Gebruikers beheren (Kritiek)", true),
            new ScopeMapping("/auth/admin.directory.group.readonly", ScopeCategory.ORG_STRUCTURE, "Groepen inzien", false),
            new ScopeMapping("/auth/admin.directory.group", ScopeCategory.ADMIN_DIRECTORY, "Groepen beheren (Kritiek)", true),
            new ScopeMapping("/auth/admin.directory.device", ScopeCategory.ADMIN_DIRECTORY, "Apparaten beheren", true),
            new ScopeMapping("/auth/admin.directory", ScopeCategory.ADMIN_DIRECTORY, "Volledige beheerdersrechten", true),
            new ScopeMapping("/auth/cloud-platform", ScopeCategory.GCP, "Infrastructuur beheren", true),

            // --- 7. WORKSPACE EXTRA'S (Groups, Licensing, Chat) ---
            new ScopeMapping("apps.groups.settings", ScopeCategory.GROUP_SETTINGS, "Beveiligingsinstellingen groepen inzien/wijzigen", false),
            new ScopeMapping("apps.licensing", ScopeCategory.LICENSING, "Licenties toewijzen/verwijderen", false),
            new ScopeMapping("/auth/chat.messages", ScopeCategory.CHAT, "Berichten lezen & sturen", true),
            new ScopeMapping("/auth/chat.spaces", ScopeCategory.CHAT, "Kanalen beheren", false),

            // --- 8. YOUTUBE ---
            new ScopeMapping("/auth/youtube.readonly", ScopeCategory.YOUTUBE, "Kanaalgegevens inzien", false),
            new ScopeMapping("/auth/youtube.upload", ScopeCategory.YOUTUBE, "Video's uploaden", false),
            new ScopeMapping("/auth/youtube.force-ssl", ScopeCategory.YOUTUBE, "Kanaal volledig beheren", true),
            new ScopeMapping("/auth/youtube", ScopeCategory.YOUTUBE, "Kanaal volledig beheren", true),

            // --- 9. APPS SCRIPT, AUTOMATISERING & DATA TRANSFER ---
            new ScopeMapping("external_request", ScopeCategory.EXTERNAL_API, "Data naar externe servers sturen (Risico op datalek)", true),
            new ScopeMapping("send_mail", ScopeCategory.APPS_SCRIPT_MAIL, "Geautomatiseerde e-mails versturen", true),
            new ScopeMapping("scriptapp", ScopeCategory.APPS_SCRIPT, "Aangepaste scripts uitvoeren", true),
            new ScopeMapping("datatransfer", ScopeCategory.DATA_TRANSFER, "Bedrijfsdata en eigenaarschap migreren/overdragen", true),
            new ScopeMapping("com/auth/flexible-api", ScopeCategory.FLEXIBLE_API, "Aangepaste API toegang", false),

            // --- FALLBACK READONLY ---
            new ScopeMapping("Readonly", ScopeCategory.READONLY, "Basis leesrechten (Specifiek domein onbekend)", false),
            new ScopeMapping("readonly", ScopeCategory.READONLY, "Basis leesrechten (Specifiek domein onbekend)", false)
    );

    public GoogleOAuthService(GoogleOAuthCacheService oAuthCacheService) {
        this.oAuthCacheService = oAuthCacheService;
    }

    public void forceRefreshCache(String loggedInEmail) {
        oAuthCacheService.forceRefreshCache(loggedInEmail);
    }

    public OAuthPagedResponse getOAuthPaged(String loggedInEmail, String pageToken, int size, String query, String risk) {
        OAuthCacheEntry cachedData = oAuthCacheService.getOrFetchOAuthData(loggedInEmail);
        int totalDomainUsers = cachedData.totalDomainUsers();

        List<AggregatedAppBuilder> builders = aggregateTokens(cachedData.allRawTokens());

        if (query != null && !query.trim().isEmpty()) {
            String lowerQuery = query.toLowerCase().trim();
            builders = builders.stream()
                    .filter(app -> app.name != null && app.name.toLowerCase().contains(lowerQuery))
                    .toList();
        }

        List<AggregatedAppBuilder> filteredBuilders = builders;

        int allFilteredApps = filteredBuilders.size();

        Map<Boolean, List<AggregatedAppBuilder>> partitioned = filteredBuilders.stream().collect(Collectors.partitioningBy(b -> isAppHighRisk(b.allScopes)));

        List<AggregatedAppBuilder> highRiskBuilders = partitioned.get(true);
        List<AggregatedAppBuilder> notHighRiskBuilders = partitioned.get(false);

        int allHighRiskApps = highRiskBuilders.size();
        int allNotHighRiskApps = notHighRiskBuilders.size();

         if (risk != null) {
            filteredBuilders = switch (risk) {
                case "high" -> highRiskBuilders;
                case "not-high" -> notHighRiskBuilders;
                default -> filteredBuilders;
            };
         }

        filteredBuilders = filteredBuilders.stream()
                .sorted(Comparator.comparing(a -> a.name))
                .toList();

        int page = GoogleServiceHelperMethods.getPage(pageToken);

        int totalApps = filteredBuilders.size();
        int startIndex = (page - 1) * size;
        int endIndex = Math.min(startIndex + size, totalApps);

        List<AggregatedAppBuilder> pagedItems = (startIndex >= totalApps)
                ? Collections.emptyList()
                : filteredBuilders.subList(startIndex, endIndex);

        List<AggregatedAppDto> mappedItems = pagedItems.stream()
                .map(builder -> mapToFrontendDto(builder, totalDomainUsers))
                .toList();

        String nextTokenToReturn = (endIndex < totalApps) ? String.valueOf(page + 1) : null;
        return new OAuthPagedResponse(mappedItems, nextTokenToReturn, allFilteredApps, allHighRiskApps, allNotHighRiskApps);
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
        List<DataAccessDto> accessList = builder.allScopes.stream()
                .map(this::translateScopeDetails)
                .distinct()
                .toList();

        int highRiskCount = (int) accessList.stream().filter(DataAccessDto::risk).count();
        boolean isHighRisk = highRiskCount > 0;

        String appType = builder.isNative ? "Geïnstalleerde App (Lokaal)" : "Web Applicatie (Cloud)";

        int exposure = 0;
        if (totalDomainUsers > 0) {
            exposure = (int) Math.round(((double) builder.userEmails.size() / totalDomainUsers) * 100);
        }

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
        if (scopeUrl == null) {
            return new DataAccessDto("Onbekend", "Geen scope data beschikbaar", false);
        }

        for (ScopeMapping mapping : SCOPE_MAPPINGS) {
            if (scopeUrl.contains(mapping.keyword())) {
                return new DataAccessDto(mapping.category().getDisplayName(), mapping.description(), mapping.isCritical());
            }
        }

        return generateFallbackDto(scopeUrl);
    }

    private DataAccessDto generateFallbackDto(String scopeUrl) {
        String fallbackName = scopeUrl.substring(scopeUrl.lastIndexOf('/') + 1);

        if (fallbackName.contains(".")) {
            fallbackName = fallbackName.substring(fallbackName.lastIndexOf('.') + 1);
        }

        if (!fallbackName.isEmpty()) {
            fallbackName = fallbackName.substring(0, 1).toUpperCase() + fallbackName.substring(1);
        }

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
