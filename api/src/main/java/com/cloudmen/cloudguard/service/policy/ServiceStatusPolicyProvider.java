package com.cloudmen.cloudguard.service.policy;

import com.cloudmen.cloudguard.domain.model.ServiceStatus;
import com.cloudmen.cloudguard.dto.ServiceStatusDto;
import com.cloudmen.cloudguard.dto.organization.OrgUnitPolicyDto;
import com.cloudmen.cloudguard.service.cache.PolicyApiCacheService;
import com.fasterxml.jackson.databind.JsonNode;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.MessageSource;
import org.springframework.context.i18n.LocaleContextHolder;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

import java.util.*;

@Order(2)
@Component
public class ServiceStatusPolicyProvider implements OrgUnitPolicyProvider {

    private static final Logger log = LoggerFactory.getLogger(ServiceStatusPolicyProvider.class);

    private static final Set<String> TARGET_SERVICES = Set.of("gmail", "drive_and_docs", "meet");

    private final PolicyApiCacheService policyCache;
    private final MessageSource messageSource;

    public ServiceStatusPolicyProvider(PolicyApiCacheService policyCache, MessageSource messageSource) {
        this.policyCache = policyCache;
        this.messageSource = messageSource;
    }

    @Override
    public String key() {
        return "ou_services_status";
    }

    @Override
    public OrgUnitPolicyDto fetch(String adminEmail, String orgUnitPath) throws Exception {
        String path = normalizeOrgUnitPath(orgUnitPath);

        List<JsonNode> allPolicies = policyCache.getAllPolicies(adminEmail);
        Map<String, String> ouMap = policyCache.getOuIdToPathMap(adminEmail);

        List<JsonNode> policies = allPolicies.stream()
                .filter(this::isServiceStatusPolicy)
                .toList();

        Map<String, ServiceStatusDto> results = new LinkedHashMap<>();
        for (String service : TARGET_SERVICES) {
            ServiceStatusDto r = resolveServiceForOu(policies, service, path, ouMap);
            results.put(service, r);
        }

        String status = formatCombinedStatus(results);
        boolean anyUnknown = results.values().stream().anyMatch(ServiceStatusDto::isUnknown);
        boolean anyOff = results.values().stream()
                .anyMatch(r -> r.getStatus() == ServiceStatus.DISABLED);
        boolean anyInherited = results.values().stream().anyMatch(ServiceStatusDto::isInherited);
        String statusClass = anyUnknown
                ? "bg-slate-100 text-slate-700"
                : anyOff ? "bg-amber-100 text-amber-800" : "bg-green-100 text-green-800";

        Locale locale = LocaleContextHolder.getLocale();

        String baseExplanation = messageSource.getMessage("orgUnits.service_status.base_explanation", null, locale);
        String description = anyUnknown
                ? messageSource.getMessage("orgUnits.service_status.any_unknown", null, locale)
                : anyOff
                        ? messageSource.getMessage("orgUnits.service_status.any_off", null, locale)
                        : messageSource.getMessage("orgUnits.service_status.any_off_not", null, locale);
        String inheritanceExplanation = anyInherited
                ? messageSource.getMessage("orgUnits.service_status.inherited", null, locale)
                : messageSource.getMessage("orgUnits.service_status.not_inherited", null, locale);

        return new OrgUnitPolicyDto(
                key(),
                "Google Workspace Services",
                description,
                status,
                statusClass,
                baseExplanation,
                inheritanceExplanation,
                anyInherited,
                "https://admin.google.com/u/1/ac/appslist/core",
                null
        );
    }

    private static String normalizeOrgUnitPath(String orgUnitPath) {
        return (orgUnitPath == null || orgUnitPath.isBlank()) ? "/" : orgUnitPath.trim();
    }

    private boolean isServiceStatusPolicy(JsonNode policy) {
        JsonNode setting = policy.get("setting");
        if (setting == null) return false;
        JsonNode type = setting.get("type");
        if (type == null) return false;
        return type.asText().endsWith("service_status");
    }

    /**
     * For a given service (e.g. "gmail"), finds all policies with matching setting.type,
     * resolves each policy's OU ID to a path, and picks the most specific ancestor
     */
    private ServiceStatusDto resolveServiceForOu(
            List<JsonNode> policies, String service, String orgUnitPath, Map<String, String> ouMap) {

        String wantedType = "settings/" + service + ".service_status";

        JsonNode best = null;
        String bestOuPath = null;
        int bestDepth = -1;

        for (JsonNode p : policies) {
            JsonNode setting = p.get("setting");
            if (setting == null) continue;
            if (!wantedType.equals(setting.path("type").asText(""))) continue;

            String ouRef = p.path("policyQuery").path("orgUnit").asText("");
            String policyOuPath = policyCache.resolveOuIdToPath(ouRef, ouMap);

            //DEBUG LOG
            //log.info("Matched type={}, ouRef={}, resolvedOuPath={}", wantedType, ouRef, policyOuPath);

            if (!isAncestorOrSelf(orgUnitPath, policyOuPath)) continue;

            int depth = depthOf(policyOuPath);
            if (depth > bestDepth) {
                best = p;
                bestOuPath = policyOuPath;
                bestDepth = depth;
            }
        }

        if (best == null) {
            log.warn("No service_status policy found for service={} and OU={}", service, orgUnitPath);
            String text = messageSource.getMessage("orgUnits.service_status.no_policy_found", null, LocaleContextHolder.getLocale());
            return new ServiceStatusDto(service, ServiceStatus.UNKNOWN, true, "("+text+")");
        }

        JsonNode valueNode = best.path("setting").path("value");

        /* DEBUG LOG
        log.info("Selected best policy for {} → OU={} value={}",
                service,
                bestOuPath,
                valueNode.toPrettyString());
         */

        boolean inherited = !orgUnitPath.equals(bestOuPath);

        ServiceStatus st = ServiceStatus.UNKNOWN;
        if (valueNode != null && !valueNode.isMissingNode() && !valueNode.isNull()) {

            // Variant A: {"enabled": true/false}
            JsonNode enabledNode = valueNode.get("enabled");
            if (enabledNode != null && !enabledNode.isNull() && enabledNode.isBoolean()) {
                st = enabledNode.asBoolean() ? ServiceStatus.ENABLED : ServiceStatus.DISABLED;
            }

            // Variant B: {"serviceState": "ENABLED" | "DISABLED"}
            if (st == ServiceStatus.UNKNOWN) {
                JsonNode stateNode = valueNode.get("serviceState");
                if (stateNode != null && !stateNode.isNull()) {
                    String state = stateNode.asText("").toUpperCase();
                    if (state.contains("ENABLED")) st = ServiceStatus.ENABLED;
                    else if (state.contains("DISABLED")) st = ServiceStatus.DISABLED;
                }
            }

            // Variant C: value itself is boolean
            if (st == ServiceStatus.UNKNOWN && valueNode.isBoolean()) {
                st = valueNode.asBoolean() ? ServiceStatus.ENABLED : ServiceStatus.DISABLED;
            }
        }

        return new ServiceStatusDto(service, st, inherited, bestOuPath);
    }

    private static boolean isAncestorOrSelf(String target, String policyOuPath) {
        if ("/".equals(policyOuPath)) return true;
        if (target.equals(policyOuPath)) return true;
        return target.startsWith(policyOuPath + "/");
    }

    private static int depthOf(String ouPath) {
        if (ouPath == null || "/".equals(ouPath)) return 0;
        int count = 0;
        for (char c : ouPath.toCharArray()) if (c == '/') count++;
        return count;
    }

    private String formatCombinedStatus(Map<String, ServiceStatusDto> results) {
        return "Gmail: " + formatStatus(results.get("gmail"))
                + " • Drive: " + formatStatus(results.get("drive_and_docs"))
                + " • Meet: " + formatStatus(results.get("meet"));
    }

    private String formatStatus(ServiceStatusDto dto) {
        Locale locale = LocaleContextHolder.getLocale();
        if (dto == null) return messageSource.getMessage("unknown", null,locale);
        return switch (dto.getStatus()) {
            case ENABLED -> messageSource.getMessage("on", null,locale);
            case DISABLED -> messageSource.getMessage("off", null,locale);
            case UNKNOWN -> messageSource.getMessage("unknown", null,locale);
        };
    }
}
