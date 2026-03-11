package com.cloudmen.cloudguard.service.policy;

import com.cloudmen.cloudguard.dto.organization.OrgUnitPolicyDto;
import com.cloudmen.cloudguard.service.cache.PolicyApiCacheService;
import com.fasterxml.jackson.databind.JsonNode;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;

/**
 * Policy provider for Chrome extension management.
 * Status logic:
 * - Managed: force-installed extensions OR (default blocked AND only certain extensions allowed)
 * - Restricted: default allow BUT block list exists, or force-installed extensions
 * - Open: default allow, no other extension rules
 */
@Order(4)
@Component
public class ChromeExtensionPolicyProvider implements OrgUnitPolicyProvider {

    private static final Logger log = LoggerFactory.getLogger(ChromeExtensionPolicyProvider.class);
    private static final String SETTINGS_LINK_TEXT = "Klik hier om deze instellingen aan te passen";
    private static final String BASE_EXPLANATION = "Deze beleidsregel toont hoe Chrome-extensies voor deze organisatie-eenheid worden beheerd.";
    private static final String ADMIN_LINK = "https://admin.google.com/u/1/ac/chrome/apps";

    private final PolicyApiCacheService policyCache;
    private final ChromePolicyApiService chromePolicyApi;

    public ChromeExtensionPolicyProvider(PolicyApiCacheService policyCache, ChromePolicyApiService chromePolicyApi) {
        this.policyCache = policyCache;
        this.chromePolicyApi = chromePolicyApi;
    }

    @Override
    public String key() {
        return "ou_chrome_extensions";
    }

    @Override
    public OrgUnitPolicyDto fetch(String adminEmail, String orgUnitPath) throws Exception {
        String path = (orgUnitPath == null || orgUnitPath.isBlank()) ? "/" : orgUnitPath.trim();
        Map<String, String> ouMap = policyCache.getOuIdToPathMap(adminEmail);
        String orgUnitId = policyCache.resolvePathToOuId(path, ouMap);

        if (orgUnitId == null) {
            return buildNotConfigured(path, false);
        }

        Map<String, JsonNode> chromePolicies;
        try {
            chromePolicies = chromePolicyApi.resolveChromePoliciesForOrgUnit(adminEmail, orgUnitId);
        } catch (Exception e) {
            log.warn("Chrome Policy API failed for OU {}: {}", path, e.getMessage());
            return buildNotConfigured(path, true);
        }

        if (chromePolicies.isEmpty()) {
            return buildNotConfigured(path, false);
        }

        ExtensionCounts counts = parseExtensionCounts(chromePolicies);
        String sourceOuPath = resolveSourceOuPath(chromePolicies, ouMap);
        boolean inherited = sourceOuPath != null && !path.equals(sourceOuPath);

        // Determine status
        boolean hasForceInstalled = counts.forceInstalled > 0;
        boolean defaultBlocked = counts.defaultBlocked;
        boolean hasAllowlist = counts.allowed > 0;
        boolean hasBlocklist = counts.blocked > 0;

        String status;
        String statusClass;
        String description;

        if (hasForceInstalled || (defaultBlocked && hasAllowlist)) {
            status = "Managed";
            statusClass = "bg-amber-100 text-amber-800";
            description = hasForceInstalled
                    ? "Er zijn force-installed extensies"
                    : "Alleen specifieke extensies zijn toegestaan";
        } else if (defaultBlocked && hasBlocklist) {
            // Default block, blocklist only (block all) - treat as Managed
            status = "Managed";
            statusClass = "bg-amber-100 text-amber-800";
            description = "Alle extensies zijn standaard geblokkeerd";
        } else if (!defaultBlocked && hasBlocklist) {
            status = "Restricted";
            statusClass = "bg-amber-100 text-amber-800";
            description = "Er is een blokkeerlijst of force-installed extensies";
        } else {
            status = "Open";
            statusClass = "bg-green-100 text-green-800";
            description = "Er zijn geen aanvullende restricties";
        }

        String inheritanceExplanation = inherited
                ? "Overgenomen van bovenliggende OU: " + sourceOuPath + "."
                : "Rechtstreeks ingesteld op deze OU.";
        String details = String.format("Geblokkeerd: %d • Force-installed: %d • Toegestaan: %d",
                counts.blocked, counts.forceInstalled, counts.allowed);

        return new OrgUnitPolicyDto(
                key(),
                "Chrome-extensies",
                description,
                status,
                statusClass,
                BASE_EXPLANATION,
                inheritanceExplanation,
                inherited,
                SETTINGS_LINK_TEXT,
                ADMIN_LINK,
                details
        );
    }

    private ExtensionCounts parseExtensionCounts(Map<String, JsonNode> chromePolicies) {
        ExtensionCounts counts = new ExtensionCounts();

        for (Map.Entry<String, JsonNode> entry : chromePolicies.entrySet()) {
            String schema = entry.getKey();
            JsonNode value = entry.getValue();

            if (schema.contains("ExtensionSettings")) {
                parseExtensionSettings(value, counts);
            } else if (schema.contains("ExtensionInstallBlocklist")) {
                parseStringList(value, counts.blockedIds);
                if (counts.blockedIds.contains("*")) counts.defaultBlocked = true;
            } else if (schema.contains("ExtensionInstallAllowlist")) {
                parseStringList(value, counts.allowedIds);
            } else if (schema.contains("ExtensionInstallForcelist")) {
                parseForcelist(value, counts);
            }
        }

        counts.blocked = counts.blockedIds.size();
        counts.allowed = counts.allowedIds.size();
        counts.forceInstalled = counts.forceInstalledIds.size();
        return counts;
    }

    private void parseExtensionSettings(JsonNode value, ExtensionCounts counts) {
        if (value == null || !value.isObject()) return;
        Iterator<String> keys = value.fieldNames();
        while (keys.hasNext()) {
            String extId = keys.next();
            JsonNode config = value.get(extId);
            if (config == null || !config.isObject()) continue;

            String mode = config.path("installation_mode").asText("allowed");
            if ("*".equals(extId)) {
                // * is the default for all extensions
                counts.defaultBlocked = "blocked".equalsIgnoreCase(mode) || "removed".equalsIgnoreCase(mode);
            } else {
                switch (mode.toLowerCase()) {
                    case "blocked", "removed" -> counts.blockedIds.add(extId);
                    case "force_installed" -> counts.forceInstalledIds.add(extId);
                    case "allowed", "normal_installed" -> counts.allowedIds.add(extId);
                    default -> { /* ignore */ }
                }
            }
        }
    }

    private void parseStringList(JsonNode value, Set<String> target) {
        if (value == null) return;
        // Chrome Policy API may return protobuf Struct: listValue.values[].stringValue
        JsonNode listNode = value.path("listValue");
        if (!listNode.isMissingNode()) {
            JsonNode values = listNode.path("values");
            if (values.isArray()) {
                for (JsonNode item : values) {
                    String s = item.path("stringValue").asText("");
                    if (!s.isBlank()) target.add(s);
                }
            }
            return;
        }
        if (value.isArray()) {
            for (JsonNode item : value) {
                String s = item.isTextual() ? item.asText() : item.path("stringValue").asText("");
                if (!s.isBlank()) target.add(s);
            }
        } else if (value.isTextual()) {
            target.add(value.asText());
        }
    }

    private void parseForcelist(JsonNode value, ExtensionCounts counts) {
        if (value == null) return;
        // Forcelist format: ["extId;updateUrl", ...] or listValue
        JsonNode listNode = value.path("listValue");
        JsonNode items = listNode.isMissingNode() ? value : listNode.path("values");
        if (!items.isArray()) return;
        for (JsonNode item : items) {
            String s = item.path("stringValue").asText(item.asText(""));
            int idx = s.indexOf(';');
            String extId = idx >= 0 ? s.substring(0, idx).trim() : s.trim();
            if (!extId.isBlank()) counts.forceInstalledIds.add(extId);
        }
    }

    private String resolveSourceOuPath(Map<String, JsonNode> chromePolicies, Map<String, String> ouMap) {
        // Chrome Policy API ResolvedPolicy has sourceKey - we don't have it in our simplified response.
        // For now, we cannot easily determine inheritance from Chrome API - assume direct.
        return null;
    }

    private OrgUnitPolicyDto buildNotConfigured(String path, boolean apiError) {
        String description = apiError
                ? "Chrome-beheer niet ingeschakeld / API niet bereikbaar"
                : "Er is geen actief extensiebeleid";
        String inheritanceExplanation = apiError
                ? "Chrome Policy API niet beschikbaar of niet ingeschakeld. Schakel Chrome-beheer in voor uw domein."
                : "Geen Chrome-extensiebeleid gevonden voor deze OU.";

        return new OrgUnitPolicyDto(
                key(),
                "Chrome-extensies",
                description,
                "Niet geconfigureerd",
                "bg-slate-100 text-slate-700",
                BASE_EXPLANATION,
                inheritanceExplanation,
                false,
                SETTINGS_LINK_TEXT,
                ADMIN_LINK,
                "Geen extensieregels gevonden."
        );
    }

    private static class ExtensionCounts {
        final Set<String> blockedIds = new HashSet<>();
        final Set<String> allowedIds = new HashSet<>();
        final Set<String> forceInstalledIds = new HashSet<>();
        boolean defaultBlocked;
        int blocked;
        int allowed;
        int forceInstalled;
    }
}
