package com.cloudmen.cloudguard.service.policy;

import com.cloudmen.cloudguard.dto.organization.OrgUnitPolicyDto;
import com.cloudmen.cloudguard.service.cache.PolicyApiCacheService;
import com.fasterxml.jackson.databind.JsonNode;
import org.springframework.context.MessageSource;
import org.springframework.context.i18n.LocaleContextHolder;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

import java.util.Locale;
import java.util.Map;

/**
 * Policy card for Shared drive creation: Allowed / Not allowed.
 * Uses Cloud Identity Policy API setting: drive_and_docs.shared_drive_creation.
 * Note: the API returns the *opposite* of the Admin Console UI for allow_shared_drive_creation.
 */
@Order(3)
@Component
public class SharedDriveCreationPolicyProvider implements OrgUnitPolicyProvider {

    private static final String SETTING_TYPE = "settings/drive_and_docs.shared_drive_creation";

    private final PolicyApiCacheService policyCache;
    private final MessageSource messageSource;

    public SharedDriveCreationPolicyProvider(PolicyApiCacheService policyCache, MessageSource messageSource) {
        this.policyCache = policyCache;
        this.messageSource = messageSource;
    }

    @Override
    public String key() {
        return "ou_shared_drive_creation";
    }

    @Override
    public OrgUnitPolicyDto fetch(String adminEmail, String orgUnitPath) throws Exception {
        String path = (orgUnitPath == null || orgUnitPath.isBlank()) ? "/" : orgUnitPath.trim();
        var allPolicies = policyCache.getAllPolicies(adminEmail);
        Map<String, String> ouMap = policyCache.getOuIdToPathMap(adminEmail);

        JsonNode best = null;
        String bestOuPath = null;
        int bestDepth = -1;
        double bestSort = Double.NEGATIVE_INFINITY;

        for (JsonNode p : allPolicies) {
            JsonNode setting = p.get("setting");
            if (setting == null) continue;

            /* log raw policy json
            if (SETTING_TYPE.equals(setting.path("type").asText(""))) {
                System.out.println("SharedDrive policy raw: " + p.toPrettyString());
            }
            */

            if (!SETTING_TYPE.equals(setting.path("type").asText(""))) continue;

            String ouRef = p.path("policyQuery").path("orgUnit").asText("");
            String policyOuPath = policyCache.resolveOuIdToPath(ouRef, ouMap);
            if (!isAncestorOrSelf(path, policyOuPath)) continue;

            int depth = depthOf(policyOuPath);
            double sortOrder = p.path("policyQuery").path("sortOrder").asDouble(0.0);

            if (depth > bestDepth || (depth == bestDepth && sortOrder > bestSort)) {
                best = p;
                bestOuPath = policyOuPath;
                bestDepth = depth;
                bestSort = sortOrder;
            }
        }

        Locale locale = LocaleContextHolder.getLocale();

        if (best == null) {
            String baseExplanation = messageSource.getMessage("orgUnits.shared_drive.create.base_explanation", null, locale);
            String dynamicExplanation = messageSource.getMessage("orgUnits.shared_drive.create.dynamic_explanation", null, locale);
            String inheritanceExplanation = messageSource.getMessage("orgUnits.shared_drive.create.inheritance_explanation", null, locale);

            return new OrgUnitPolicyDto(
                    key(),
                    messageSource.getMessage("orgUnits.shared_drive.create.title", null, locale),
                    messageSource.getMessage("orgUnits.shared_drive.create.description", null, locale),
                    messageSource.getMessage("orgUnits.not_configured", null, locale),
                    "bg-slate-100 text-slate-700",
                    baseExplanation + " " + dynamicExplanation,
                    inheritanceExplanation,
                    false,
                    "apps",
                    null
            );
        }

        JsonNode value = best.path("setting").path("value");
        JsonNode allowedNode = value.path("allowSharedDriveCreation");
        boolean inherited = !path.equals(bestOuPath);

        String status;
        String statusClass;
        String description;

        String baseExplanation = messageSource.getMessage("orgUnits.shared_drive.base_explanation", null, locale);
        String inheritanceExplanation;

        if (allowedNode.isMissingNode() || allowedNode.isNull()) {
            status = messageSource.getMessage("orgUnits.shared_drive.status.not_found", null, locale);
            statusClass = "bg-slate-100 text-slate-700";
            description = messageSource.getMessage("orgUnits.shared_drive.description.not_found", null, locale);
            inheritanceExplanation = messageSource.getMessage("orgUnits.shared_drive.inheritance.not_found", null, locale);
        } else {
            boolean allowed = allowedNode.asBoolean();
            status = allowed ? messageSource.getMessage("allowed", null, locale) : messageSource.getMessage("not_allowed", null, locale);
            statusClass = allowed ? "bg-green-100 text-green-800" : "bg-amber-100 text-amber-800";
            description = allowed
                    ? messageSource.getMessage("orgUnits.shared_drive.description.allowed", null, locale)
                    : messageSource.getMessage("orgUnits.shared_drive.description.not_allowed", null, locale);
            inheritanceExplanation = inherited
                    ? messageSource.getMessage("orgUnits.shared_drive.inherited", null, locale)
                    : messageSource.getMessage("orgUnits.shared_drive.not_inherited", null, locale);
        }

        return new OrgUnitPolicyDto(
                key(),
                "Shared Drives",
                description,
                status,
                statusClass,
                baseExplanation,
                inheritanceExplanation,
                inherited,
                "https://admin.google.com/u/1/ac/managedsettings/55656082996",
                null
        );
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
}
