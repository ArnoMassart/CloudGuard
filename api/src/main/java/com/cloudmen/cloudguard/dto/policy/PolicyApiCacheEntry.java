package com.cloudmen.cloudguard.dto.policy;

import com.fasterxml.jackson.databind.JsonNode;

import java.util.List;
import java.util.Map;

/**
 * Cached snapshot consumed by OU policy providers ({@link com.cloudmen.cloudguard.service.policy.SharedDriveCreationPolicyProvider},
 * {@link com.cloudmen.cloudguard.service.policy.ChromeExtensionPolicyProvider} via path/id resolution): paginated Cloud Identity
 * policies JSON nodes plus a Directory-derived map from org-unit id to {@link com.google.api.services.admin.directory.model.OrgUnit#getOrgUnitPath()}.
 *
 * @param policies   raw policy objects from {@code cloudidentity.googleapis.com/v1/policies}
 * @param ouIdToPath keys are Directory {@link com.google.api.services.admin.directory.model.OrgUnit#getOrgUnitId()}; values are OU paths
 */
public record PolicyApiCacheEntry(List<JsonNode> policies, Map<String, String> ouIdToPath) {}
