package com.cloudmen.cloudguard.service.policy;

import com.cloudmen.cloudguard.dto.organization.OrgUnitPolicyDto;
import com.fasterxml.jackson.databind.JsonNode;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

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

    public SharedDriveCreationPolicyProvider(PolicyApiCacheService policyCache) {
        this.policyCache = policyCache;
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

        if (best == null) {
            String baseExplanation = "Deze beleidsregel bepaalt wie nieuwe gedeelde drives mag aanmaken binnen deze organisatie-eenheid.";
            String inheritanceExplanation = "Geen beleid voor gedeelde drive-aanmaak gevonden voor deze OU of bovenliggende OUs.";

            return new OrgUnitPolicyDto(
                    key(),
                    "Gedeelde drives aanmaken",
                    "Wie mag nieuwe gedeelde drives aanmaken",
                    "Niet geconfigureerd",
                    "bg-slate-100 text-slate-700",
                    baseExplanation,
                    inheritanceExplanation,
                    false,
                    "Klik hier om deze instellingen aan te passen",
                    "apps",
                    null
            );
        }

        JsonNode value = best.path("setting").path("value");
        JsonNode allowedNode = value.path("allowSharedDriveCreation");
        boolean inherited = !path.equals(bestOuPath);

        String status;
        String statusClass;

        String baseExplanation = "Shared Drives zorgen dat bestanden eigendom blijven van de organisatie i.p.v. individuele gebruikers.";
        String inheritanceExplanation;

        if (allowedNode.isMissingNode() || allowedNode.isNull()) {
            status = "Kon niet bepalen";
            statusClass = "bg-slate-100 text-slate-700";
            inheritanceExplanation = "Beleid gevonden maar waarde ontbreekt in API-response.";
        } else {
            boolean allowed = allowedNode.asBoolean();
            status = allowed ? "Toegestaan" : "Niet toegestaan";
            statusClass = allowed ? "bg-green-100 text-green-800" : "bg-amber-100 text-amber-800";
            inheritanceExplanation = inherited
                    ? "Overgenomen van bovenliggende OU."
                    : "Rechtstreeks ingesteld op deze OU.";
        }

        return new OrgUnitPolicyDto(
                key(),
                "Shared Drives",
                "Wie mag nieuwe gedeelde drives aanmaken",
                status,
                statusClass,
                baseExplanation,
                inheritanceExplanation,
                inherited,
                "Klik hier om deze instellingen aan te passen",
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
