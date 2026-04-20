package com.cloudmen.cloudguard.service;

import com.cloudmen.cloudguard.dto.oauth.*;
import com.cloudmen.cloudguard.dto.password.SecurityScoreBreakdownDto;
import com.cloudmen.cloudguard.dto.password.SecurityScoreFactorDto;
import com.cloudmen.cloudguard.service.cache.GoogleOAuthCacheService;
import com.cloudmen.cloudguard.service.preference.SecurityPreferenceScoreSupport;
import com.cloudmen.cloudguard.utility.GoogleServiceHelperMethods;
import org.springframework.context.MessageSource;
import org.springframework.context.i18n.LocaleContextHolder;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.stream.Collectors;

import static com.cloudmen.cloudguard.utility.GoogleServiceHelperMethods.severity;

@Service
public class GoogleOAuthService {
    private final GoogleOAuthCacheService oAuthCacheService;

    private static final List<String> INTERNAL_CLIENT_IDS = List.of(
            "1046230096039-0c4fqtecqs3fe7t762cn1922garmr4p5.apps.googleusercontent.com"
    );

    private static final String FULL_ACCESS_READ_WRITE_TEXT = "app-access.rights.full-access";
    private final MessageSource messageSource;

    private record ScopeMapping(String keyword, ScopeCategory category, String description, boolean isCritical) {}

    private static final List<ScopeMapping> SCOPE_MAPPINGS = List.of(
            // --- 1. IDENTITEIT & INLOGGEN (SSO) ---
            new ScopeMapping("openid", ScopeCategory.SIGN_IN, "app-access.rights.basic-profile", false),
            new ScopeMapping("userinfo.profile", ScopeCategory.SIGN_IN, "app-access.rights.basic-profile", false),
            new ScopeMapping("userinfo.email", ScopeCategory.SIGN_IN, "app-access.rights.email-viewing", false),

            // --- 2. GMAIL ---
            new ScopeMapping("/auth/gmail.readonly", ScopeCategory.GMAIL, "app-access.rights.email-reading", false),
            new ScopeMapping("/auth/gmail.send", ScopeCategory.GMAIL, "app-access.rights.sending-messages", false),
            new ScopeMapping("/auth/gmail.compose", ScopeCategory.GMAIL, "app-access.rights.making-concepts", false),
            new ScopeMapping("/auth/gmail.modify", ScopeCategory.GMAIL, "app-access.rights.email-changing", true),
            new ScopeMapping("/auth/gmail.settings.basic", ScopeCategory.GMAIL_SETTINGS, "app-access.rights.mailbox-settings", false),
            new ScopeMapping("/auth/gmail", ScopeCategory.GMAIL, FULL_ACCESS_READ_WRITE_TEXT, true),
            new ScopeMapping("/mail.google.com", ScopeCategory.GMAIL, FULL_ACCESS_READ_WRITE_TEXT, true),

            // --- 3. GOOGLE DRIVE ---
            new ScopeMapping("/auth/drive.readonly", ScopeCategory.DRIVE, "app-access.rights.reading-files", false),
            new ScopeMapping("/auth/drive.file", ScopeCategory.DRIVE, "app-access.rights.only-own-files", false),
            new ScopeMapping("/auth/drive.appdata", ScopeCategory.DRIVE, "app-access.rights.hidden-app-saving", false),
            new ScopeMapping("/auth/drive.metadata", ScopeCategory.DRIVE, "app-access.rights.file-names-viewing", false),
            new ScopeMapping("/auth/drive", ScopeCategory.DRIVE, FULL_ACCESS_READ_WRITE_TEXT, true),

            // --- 4. OFFICE APPS (Docs, Sheets, Forms) ---
            new ScopeMapping("/auth/spreadsheets.readonly", ScopeCategory.SHEETS, "app-access.rights.viewing-spreadsheets", false),
            new ScopeMapping("/auth/spreadsheets", ScopeCategory.SHEETS, "app-access.rights.editing-spreadsheets", true),
            new ScopeMapping("/auth/documents.readonly", ScopeCategory.DOCS, "app-access.rights.viewing-documents", false),
            new ScopeMapping("/auth/documents", ScopeCategory.DOCS, "app-access.rights.editing-documents", true),
            new ScopeMapping("/auth/forms.responses.readonly", ScopeCategory.FORMS, "app-access.rights.forms-answers-viewing", false),
            new ScopeMapping("/auth/forms", ScopeCategory.FORMS, "app-access.rights.forms-managing", true),

            // --- 5. AGENDA & CONTACTEN ---
            new ScopeMapping("/auth/calendar.readonly", ScopeCategory.CALENDAR, "app-access.rights.reading-agenda", false),
            new ScopeMapping("/auth/calendar.events", ScopeCategory.CALENDAR, "app-access.rights.appointment-managing", false),
            new ScopeMapping("/auth/calendar", ScopeCategory.CALENDAR, "app-access.rights.full-agenda-access", true),
            new ScopeMapping("/auth/contacts.readonly", ScopeCategory.CONTACTS, "app-access.rights.contact-person-viewing", false),
            new ScopeMapping("/auth/contacts.other.readonly", ScopeCategory.CONTACTS, "app-access.rights.shared-contacts-viewing", false),
            new ScopeMapping("/auth/contacts", ScopeCategory.CONTACTS, "app-access.rights.contact-person-managing", true),

            // --- 6. BEHEERDERS & DIRECTORY (KRITIEK!) ---
            new ScopeMapping("/auth/admin.directory.user.readonly", ScopeCategory.ORG_STRUCTURE, "app-access.rights.users-list-viewing", false),
            new ScopeMapping("/auth/admin.directory.user", ScopeCategory.ADMIN_DIRECTORY, "app-access.rights.users-managing", true),
            new ScopeMapping("/auth/admin.directory.group.readonly", ScopeCategory.ORG_STRUCTURE, "app-access.rights.viewing-groups", false),
            new ScopeMapping("/auth/admin.directory.group", ScopeCategory.ADMIN_DIRECTORY, "app-access.rights.managing-groups", true),
            new ScopeMapping("/auth/admin.directory.device", ScopeCategory.ADMIN_DIRECTORY, "app-access.rights.managing-devices", true),
            new ScopeMapping("/auth/admin.directory", ScopeCategory.ADMIN_DIRECTORY, "app-access.rights.full-managing", true),
            new ScopeMapping("/auth/cloud-platform", ScopeCategory.GCP, "app-access.rights.infrastructure-managing", true),

            // --- 7. WORKSPACE EXTRA'S (Groups, Licensing, Chat) ---
            new ScopeMapping("apps.groups.settings", ScopeCategory.GROUP_SETTINGS, "app-access.rights.security-settings", false),
            new ScopeMapping("apps.licensing", ScopeCategory.LICENSING, "app-access.rights.licensing", false),
            new ScopeMapping("/auth/chat.messages", ScopeCategory.CHAT, "app-access.rights.messages", true),
            new ScopeMapping("/auth/chat.spaces", ScopeCategory.CHAT, "app-access.rights.chat-spaces", false),

            // --- 8. YOUTUBE ---
            new ScopeMapping("/auth/youtube.readonly", ScopeCategory.YOUTUBE, "app-access.rights.youtube-viewing", false),
            new ScopeMapping("/auth/youtube.upload", ScopeCategory.YOUTUBE, "app-access.rights.youtube-upload", false),
            new ScopeMapping("/auth/youtube.force-ssl", ScopeCategory.YOUTUBE, "app-access.rights.youtube-full", true),
            new ScopeMapping("/auth/youtube", ScopeCategory.YOUTUBE, "app-access.rights.youtube-full", true),

            // --- 9. APPS SCRIPT, AUTOMATISERING & DATA TRANSFER ---
            new ScopeMapping("external_request", ScopeCategory.EXTERNAL_API, "app-access.rights.external-request", true),
            new ScopeMapping("send_mail", ScopeCategory.APPS_SCRIPT_MAIL, "app-access.rights.send-mail", true),
            new ScopeMapping("scriptapp", ScopeCategory.APPS_SCRIPT, "app-access.rights.script-app", true),
            new ScopeMapping("datatransfer", ScopeCategory.DATA_TRANSFER, "app-access.rights.data-transfer", true),
            new ScopeMapping("com/auth/flexible-api", ScopeCategory.FLEXIBLE_API, "app-access.rights.flexible-api", false),

            // --- FALLBACK READONLY ---
            new ScopeMapping("Readonly", ScopeCategory.READONLY, "app-access.rights.basic-reading", false),
            new ScopeMapping("readonly", ScopeCategory.READONLY, "app-access.rights.basic-reading", false)
    );

    public GoogleOAuthService(GoogleOAuthCacheService oAuthCacheService, MessageSource messageSource) {
        this.oAuthCacheService = oAuthCacheService;
        this.messageSource = messageSource;
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

    public OAuthOverviewResponse getOAuthPageOverview(String loggedInEmail, Set<String> disabledKeys) {
        Set<String> off = disabledKeys == null ? Set.of() : disabledKeys;
        OAuthCacheEntry cachedData = oAuthCacheService.getOrFetchOAuthData(loggedInEmail);
        List<RawUserToken> rawTokens = cachedData.allRawTokens();

        List<AggregatedAppBuilder> apps = aggregateTokens(rawTokens);

        long totalThirdPartyApps = apps.stream().filter(app -> !(app.isAnonymous || INTERNAL_CLIENT_IDS.contains(app.clientId))).count();
        long totalPermissionsGranted = rawTokens.size();

        long totalHighRiskApps = apps.stream()
                .filter(app -> isAppHighRisk(app.allScopes))
                .count();

        boolean ignHighRisk = SecurityPreferenceScoreSupport.preferenceDisabled(off, "app-access", "highRisk");

        int securityScore = 100;
        if (!ignHighRisk && totalThirdPartyApps > 0) {
            double penalty = ((double) totalHighRiskApps / totalThirdPartyApps) * 100;
            securityScore = (int) Math.max(0, 100 - Math.round(penalty));
        }

        if (apps.isEmpty()) {
            securityScore = 0;
        }

        SecurityScoreBreakdownDto breakdown = buildOAuthBreakdown(totalThirdPartyApps, totalHighRiskApps, totalPermissionsGranted, securityScore, ignHighRisk);

        return new OAuthOverviewResponse(
                totalThirdPartyApps,
                totalHighRiskApps,
                totalPermissionsGranted,
                securityScore,
                breakdown
        );
    }

    private SecurityScoreBreakdownDto buildOAuthBreakdown(long totalThirdPartyApps, long totalHighRiskApps, long totalPermissionsGranted, int securityScore,
                                                         boolean ignoreHighRiskPref) {
        int highRiskScore = totalThirdPartyApps == 0 ? 100 : (int) Math.round((totalThirdPartyApps - totalHighRiskApps) * 100.0 / totalThirdPartyApps);
        if (ignoreHighRiskPref) {
            highRiskScore = 100;
        }
        int noAppsScore = 100;

        Locale locale = LocaleContextHolder.getLocale();

        var factors = java.util.List.of(
                new SecurityScoreFactorDto(messageSource.getMessage("apps.score.factor.without_high_risk.title", null, locale),
                        totalHighRiskApps == 0
                                ? messageSource.getMessage("apps.score.factor.without_high_risk.description.none", null, locale)
                                : messageSource.getMessage("apps.score.factor.without_high_risk.description", new Object[]{totalHighRiskApps, totalThirdPartyApps}, locale),
                        highRiskScore, 100, severity(highRiskScore), ignoreHighRiskPref),
                new SecurityScoreFactorDto("3rd-party apps", totalThirdPartyApps == 0 ? messageSource.getMessage("apps.score.factor.third_party.description.none", null, locale) : messageSource.getMessage("apps.score.factor.third_party.description", new Object[]{totalThirdPartyApps, totalPermissionsGranted}, locale), noAppsScore, 100, severity(noAppsScore), false)
        );
        String status = securityScore == 100 ? "perfect" : securityScore >= 75 ? "good" : securityScore > 50 ? "average" : "bad";
        return new SecurityScoreBreakdownDto(securityScore, status, factors);
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

        Locale locale = LocaleContextHolder.getLocale();

        String appType = builder.isNative ? messageSource.getMessage("apps.details.type.is_native", null, locale) : messageSource.getMessage("apps.details.type.is_not_native", null, locale);

        int exposure = 0;
        if (totalDomainUsers > 0) {
            exposure = (int) Math.round(((double) builder.userEmails.size() / totalDomainUsers) * 100);
        }

        boolean isInternalApp = builder.isAnonymous || INTERNAL_CLIENT_IDS.contains(builder.clientId);
        boolean isThirdParty = !isInternalApp;
        String appSource = isThirdParty ? "Third party" : messageSource.getMessage("apps.details.source.internal", null, locale);

        return new AggregatedAppDto(
                builder.clientId,
                builder.name != null ? builder.name : messageSource.getMessage("apps.details.name.unknown", null, locale),
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
        Locale locale = LocaleContextHolder.getLocale();

        if (scopeUrl == null) {
            return new DataAccessDto(messageSource.getMessage("unknown", null, locale), messageSource.getMessage("apps.details.scope.rights.no_scope", null, locale), false);
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

        Locale locale = LocaleContextHolder.getLocale();

        return new DataAccessDto(messageSource.getMessage("app.details.fallback.name", new Object[]{fallbackName}, locale), messageSource.getMessage("app.details.fallback.rights", null, locale), false);
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
