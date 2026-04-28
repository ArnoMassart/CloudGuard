package com.cloudmen.cloudguard.service.oauth;

import com.cloudmen.cloudguard.dto.oauth.*;
import com.cloudmen.cloudguard.dto.password.SecurityScoreBreakdownDto;
import com.cloudmen.cloudguard.service.cache.GoogleOAuthCacheService;
import com.cloudmen.cloudguard.service.preference.SecurityPreferenceScoreSupport;
import com.cloudmen.cloudguard.utility.GoogleServiceHelperMethods;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.stream.Collectors;

import static com.cloudmen.cloudguard.service.oauth.OAuthScopeMapper.INTERNAL_CLIENT_IDS;

/**
 * The main orchestration service for Google Workspace OAuth token analysis. <p>
 *
 * This service manages the retrieval, aggregation, filtering, and pagination of third-party applications authorized
 * by domain users. To adhere to the Single Responsibility Principle (SRP), it delegates complex scope translation and
 * compliance scoring logic to the {@link OAuthScopeMapper} and {@link OAuthComplianceScorer}.
 */
@Service
public class GoogleOAuthService {
    private final GoogleOAuthCacheService oAuthCacheService;
    private final OAuthScopeMapper mapper;
    private final OAuthComplianceScorer scorer;

    public GoogleOAuthService(GoogleOAuthCacheService oAuthCacheService, OAuthScopeMapper mapper, OAuthComplianceScorer scorer) {
        this.oAuthCacheService = oAuthCacheService;
        this.mapper = mapper;
        this.scorer = scorer;
    }

    /**
     * Triggers a manual background refresh of the OAuth token cache for the specified user.
     *
     * @param loggedInEmail the email of the authenticated user
     */
    public void forceRefreshCache(String loggedInEmail) {
        oAuthCacheService.forceRefreshCache(loggedInEmail);
    }

    /**
     * Retrieves a paginated, filtered, and aggregated list of OAuth applications. <p>
     *
     * This method processes raw user tokens into distinct applications, applies text and risk-based filters, and
     * converts the final page into frontend-ready DTOs using the injected mapper.
     *
     * @param loggedInEmail the email of the authenticated user
     * @param pageToken     the string representation of the requested page number
     * @param size          the number of applications per page
     * @param query         an optional search string to filter by application name
     * @param risk          an optional filter to show only "high" or "not-high" risk apps
     * @return an {@link OAuthPagedResponse} containing the requested page and metadata
     */
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

        Map<Boolean, List<AggregatedAppBuilder>> partitioned = filteredBuilders.stream().collect(Collectors.partitioningBy(b -> scorer.isAppHighRisk(b.allScopes)));

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
                .map(builder ->  mapper.mapToFrontendDto(builder, totalDomainUsers))
                .toList();

        String nextTokenToReturn = (endIndex < totalApps) ? String.valueOf(page + 1) : null;
        return new OAuthPagedResponse(mappedItems, nextTokenToReturn, allFilteredApps, allHighRiskApps, allNotHighRiskApps);
    }

    /**
     * Retrieves a high-level security overview of all third-party app access. <p>
     *
     * Delegates the actual score and breakdown calculations to the {@link OAuthComplianceScorer}, taking user-specific
     * security preferences into account.
     *
     * @param loggedInEmail the email of the authenticated user
     * @param disabledKeys  set of security checks to ignore based on preferences
     * @return an {@link OAuthOverviewResponse} containing total counts and scores
     */
    public OAuthOverviewResponse getOAuthPageOverview(String loggedInEmail, Set<String> disabledKeys) {
        Set<String> off = disabledKeys == null ? Set.of() : disabledKeys;
        OAuthCacheEntry cachedData = oAuthCacheService.getOrFetchOAuthData(loggedInEmail);
        List<RawUserToken> rawTokens = cachedData.allRawTokens();

        List<AggregatedAppBuilder> apps = aggregateTokens(rawTokens);

        long totalThirdPartyApps = apps.stream().filter(app -> !(app.isAnonymous || INTERNAL_CLIENT_IDS.contains(app.clientId))).count();
        long totalPermissionsGranted = rawTokens.size();

        long totalHighRiskApps = apps.stream()
                .filter(app -> scorer.isAppHighRisk(app.allScopes))
                .count();

        boolean ignHighRisk = SecurityPreferenceScoreSupport.preferenceDisabled(off, "app-access", "highRisk");

        int securityScore = scorer.calculateSecurityScore(totalThirdPartyApps, totalHighRiskApps, ignHighRisk);

        SecurityScoreBreakdownDto breakdown = scorer.buildOAuthBreakdown(totalThirdPartyApps, totalHighRiskApps, totalPermissionsGranted, securityScore, ignHighRisk);

        return new OAuthOverviewResponse(
                totalThirdPartyApps,
                totalHighRiskApps,
                totalPermissionsGranted,
                securityScore,
                breakdown
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
}
