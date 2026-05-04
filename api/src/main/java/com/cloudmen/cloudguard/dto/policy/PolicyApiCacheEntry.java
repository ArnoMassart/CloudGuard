package com.cloudmen.cloudguard.dto.policy;

import com.fasterxml.jackson.databind.JsonNode;

import java.util.List;
import java.util.Map;

/**
 * Cached Policy API payload plus OU id→path mapping for one Workspace admin (tenant).
 */
public record PolicyApiCacheEntry(
        List<JsonNode> policies,
        Map<String, String> ouIdToPath
) {
}
