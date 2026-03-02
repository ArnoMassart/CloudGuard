package com.cloudmen.cloudguard.service;

import com.cloudmen.cloudguard.dto.oAuth.*;
import com.cloudmen.cloudguard.service.cache.GoogleOAuthCacheService;
import com.cloudmen.cloudguard.utility.GoogleServiceHelperMethods;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.*;

@Service
public class GoogleOAuthService {
    private static final Logger log = LoggerFactory.getLogger(GoogleOAuthService.class);
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

        if (risk != null) {
            switch (risk) {
                case "high" -> filteredBuilders = builders.stream().filter(builder -> isAppHighRisk(builder.allScopes)).toList();
                case "not-high" -> filteredBuilders = builders.stream().filter(builder -> !isAppHighRisk(builder.allScopes)).toList();
                default -> {}
            }
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

        long totalApps = apps.size();

        return new OAuthOverviewResponse(
                totalThirdPartyApps,
                totalHighRiskApps,
                totalPermissionsGranted,
                securityScore,
                totalApps
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
        // --- 1. IDENTITEIT & INLOGGEN (SSO) ---
        if (scopeUrl.contains("openid") || scopeUrl.contains("userinfo.profile")) return new DataAccessDto("Google Sign-In", "Basis profielgegevens", false);
        if (scopeUrl.contains("userinfo.email")) return new DataAccessDto("Google Sign-In", "E-mailadres inzien", false);

        // --- 2. GMAIL ---
        if (scopeUrl.contains("/auth/gmail.readonly")) return new DataAccessDto("Gmail", "E-mails lezen", false);
        if (scopeUrl.contains("/auth/gmail.send")) return new DataAccessDto("Gmail", "Berichten verzenden", false);
        if (scopeUrl.contains("/auth/gmail.compose")) return new DataAccessDto("Gmail", "Concepten aanmaken", false);
        if (scopeUrl.contains("/auth/gmail.modify")) return new DataAccessDto("Gmail", "E-mails wijzigen/verwijderen", true);
        if (scopeUrl.contains("/auth/gmail.settings.basic")) return new DataAccessDto("Gmail Instellingen", "Mailbox instellingen inzien", false);
        if (scopeUrl.contains("/auth/gmail") || scopeUrl.contains("/mail.google.com")) return new DataAccessDto("Gmail", "Volledige toegang (Lezen/Schrijven)", true);

        // --- 3. GOOGLE DRIVE ---
        if (scopeUrl.contains("/auth/drive.readonly")) return new DataAccessDto("Google Drive", "Alle bestanden lezen", false);
        if (scopeUrl.contains("/auth/drive.file")) return new DataAccessDto("Google Drive", "Alleen eigen gemaakte bestanden inzien", false);
        if (scopeUrl.contains("/auth/drive.appdata")) return new DataAccessDto("Google Drive", "Verborgen app-data opslaan", false);
        if (scopeUrl.contains("/auth/drive.metadata")) return new DataAccessDto("Google Drive", "Bestandsnamen & mappenstructuur inzien", false);
        if (scopeUrl.contains("/auth/drive")) return new DataAccessDto("Google Drive", "Volledige toegang (Lezen/Schrijven)", true);

        // --- 4. OFFICE APPS (Docs, Sheets, Forms) ---
        if (scopeUrl.contains("/auth/spreadsheets.readonly")) return new DataAccessDto("Google Spreadsheets", "Spreadsheets inzien", false);
        if (scopeUrl.contains("/auth/spreadsheets")) return new DataAccessDto("Google Spreadsheets", "Spreadsheets bewerken", true);
        if (scopeUrl.contains("/auth/documents.readonly")) return new DataAccessDto("Google Documenten", "Documenten inzien", false);
        if (scopeUrl.contains("/auth/documents")) return new DataAccessDto("Google Documenten", "Documenten bewerken", true);
        if (scopeUrl.contains("/auth/forms.responses.readonly")) return new DataAccessDto("Google Formulieren", "Ingevulde antwoorden inzien", false);
        if (scopeUrl.contains("/auth/forms")) return new DataAccessDto("Google Formulieren", "Formulieren beheren", true);

        // --- 5. AGENDA & CONTACTEN ---
        if (scopeUrl.contains("/auth/calendar.readonly")) return new DataAccessDto("Google Agenda", "Agenda lezen", false);
        if (scopeUrl.contains("/auth/calendar.events")) return new DataAccessDto("Google Agenda", "Afspraken beheren", false);
        if (scopeUrl.contains("/auth/calendar")) return new DataAccessDto("Google Agenda", "Volledige agenda-toegang", true);
        if (scopeUrl.contains("/auth/contacts.readonly")) return new DataAccessDto("Google Contacten", "Contactpersonen inzien", false);
        if (scopeUrl.contains("/auth/contacts.other.readonly")) return new DataAccessDto("Google Contacten", "Gedeelde contacten inzien", false);
        if (scopeUrl.contains("/auth/contacts")) return new DataAccessDto("Google Contacten", "Contactpersonen beheren", true);

        // --- 6. BEHEERDERS & DIRECTORY (KRITIEK!) ---
        if (scopeUrl.contains("/auth/admin.directory.user.readonly")) return new DataAccessDto("Organisatiestructuur", "Gebruikerslijst inzien", false);
        if (scopeUrl.contains("/auth/admin.directory.user")) return new DataAccessDto("Admin Directory", "Gebruikers beheren (Kritiek)", true);
        if (scopeUrl.contains("/auth/admin.directory.group.readonly")) return new DataAccessDto("Organisatiestructuur", "Groepen inzien", false);
        if (scopeUrl.contains("/auth/admin.directory.group")) return new DataAccessDto("Admin Directory", "Groepen beheren (Kritiek)", true);
        if (scopeUrl.contains("/auth/admin.directory.device")) return new DataAccessDto("Admin Directory", "Apparaten beheren", true);
        if (scopeUrl.contains("/auth/admin.directory")) return new DataAccessDto("Admin Directory", "Volledige beheerdersrechten", true);
        if (scopeUrl.contains("/auth/cloud-platform")) return new DataAccessDto("Google Cloud (GCP)", "Infrastructuur beheren", true);

        // --- 7. WORKSPACE EXTRA'S (Groups, Licensing, Chat) ---
        if (scopeUrl.contains("apps.groups.settings")) return new DataAccessDto("Groepsinstellingen", "Beveiligingsinstellingen groepen inzien/wijzigen", false);
        if (scopeUrl.contains("apps.licensing")) return new DataAccessDto("Google Workspace Licenties", "Licenties toewijzen/verwijderen", false);
        if (scopeUrl.contains("/auth/chat.messages")) return new DataAccessDto("Google Chat", "Berichten lezen & sturen", true);
        if (scopeUrl.contains("/auth/chat.spaces")) return new DataAccessDto("Google Chat", "Kanalen beheren", false);

        // --- 8. YOUTUBE ---
        if (scopeUrl.contains("/auth/youtube.readonly")) return new DataAccessDto("YouTube", "Kanaalgegevens inzien", false);
        if (scopeUrl.contains("/auth/youtube.upload")) return new DataAccessDto("YouTube", "Video's uploaden", false);
        if (scopeUrl.contains("/auth/youtube.force-ssl") || scopeUrl.contains("/auth/youtube")) return new DataAccessDto("YouTube", "Kanaal volledig beheren", true);

        // --- 9. APPS SCRIPT, AUTOMATISERING & DATA TRANSFER (NIEUW!) ---
        if (scopeUrl.contains("external_request")) return new DataAccessDto("Externe API Verbindingen", "Data naar externe servers sturen (Risico op datalek)", true);
        if (scopeUrl.contains("send_mail")) return new DataAccessDto("Apps Script Mail", "Geautomatiseerde e-mails versturen", true);
        if (scopeUrl.contains("scriptapp")) return new DataAccessDto("Google Apps Script", "Aangepaste scripts uitvoeren", true);
        if (scopeUrl.contains("datatransfer")) return new DataAccessDto("Data Overdracht (Admin)", "Bedrijfsdata en eigenaarschap migreren/overdragen", true);
        if (scopeUrl.contains("com/auth/flexible-api")) return new DataAccessDto("Google Flexible API", "Aangepaste API toegang", false);

        // Specifieke afvanging voor "Readonly" (zonder dat we 'drive.readonly' of 'gmail.readonly' per ongeluk overschrijven)
        // We zetten deze bewust hier onderaan, zodat de specifiekere Readonly's hierboven eerst worden afgevangen!
        if (scopeUrl.contains("Readonly") || scopeUrl.contains("readonly")) return new DataAccessDto("Algemeen - Alleen Lezen", "Basis leesrechten (Specifiek domein onbekend)", false);


        // --- FALLBACK ---
        // Pakt het laatste woord uit de URL als we de scope niet herkennen (bijv. "apps.licensing" -> "licensing")
        String fallbackName = scopeUrl.substring(scopeUrl.lastIndexOf('/') + 1);

        // Soms gebruiken Google's interne API's punten in plaats van slashes. We schonen dit een beetje op:
        if (fallbackName.contains(".")) {
            fallbackName = fallbackName.substring(fallbackName.lastIndexOf('.') + 1);
        }

        // Maak de eerste letter een hoofdletter voor een mooie weergave
        fallbackName = fallbackName.substring(0, 1).toUpperCase() + fallbackName.substring(1);

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
