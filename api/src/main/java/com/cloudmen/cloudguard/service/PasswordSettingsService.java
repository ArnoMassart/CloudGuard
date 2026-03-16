package com.cloudmen.cloudguard.service;

import com.cloudmen.cloudguard.dto.password.*;
import com.cloudmen.cloudguard.service.cache.GoogleUsersCacheService;
import com.cloudmen.cloudguard.service.cache.PolicyApiCacheService;
import com.fasterxml.jackson.databind.JsonNode;
import com.google.api.services.admin.directory.model.User;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.stream.Collectors;

@Service
public class PasswordSettingsService {
    private static final Logger log = LoggerFactory.getLogger(PasswordSettingsService.class);

    private final PolicyApiCacheService policyCache;
    private final GoogleUsersCacheService usersCache;

    public PasswordSettingsService(PolicyApiCacheService policyCache,
                                  GoogleUsersCacheService usersCache) {
        this.policyCache = policyCache;
        this.usersCache = usersCache;
    }

    public PasswordSettingsDto getPasswordSettings(String adminEmail) {
        var entry = usersCache.getOrFetchUsersData(adminEmail);
        List<User> allUsers = entry.allUsers();

        PasswordPolicyDto policy = extractPasswordPolicy(adminEmail);
        TwoStepVerificationDto twoStepVerification = buildTwoStepVerification(adminEmail, allUsers);
        List<PasswordChangeRequirementDto> forcedChange = getUsersWithForcedPasswordChange(allUsers);

        int total = allUsers.size();
        int enrolled = (int) allUsers.stream().filter(u -> Boolean.TRUE.equals(u.getIsEnrolledIn2Sv())).count();
        int enforced = (int) allUsers.stream().filter(u -> Boolean.TRUE.equals(u.getIsEnforcedIn2Sv())).count();

        PasswordSettingsSummaryDto summary = new PasswordSettingsSummaryDto(
                forcedChange.size(), enrolled, enforced, total);

        return new PasswordSettingsDto(policy, twoStepVerification, forcedChange, summary);
    }

    private PasswordPolicyDto extractPasswordPolicy(String adminEmail) {
        try {
            List<JsonNode> policies = policyCache.getAllPolicies(adminEmail);
            for (JsonNode p : policies) {
                JsonNode setting = p.get("setting");
                if (setting == null) continue;
                String type = setting.path("type").asText("");
                if (!type.contains("password")) continue;

                JsonNode value = setting.get("value");
                if (value == null) return defaultPolicy();

                return new PasswordPolicyDto(
                        value.path("minLength").asInt(8),
                        value.path("maxLength").asInt(100),
                        value.path("requireLowercase").asBoolean(false),
                        value.path("requireUppercase").asBoolean(false),
                        value.path("requireNumeric").asBoolean(false),
                        value.path("requireSpecialChar").asBoolean(false),
                        value.path("expirationDays").asInt(0),
                        value.path("reusePreventionCount").asInt(0),
                        "Beleid van Policy API"
                );
            }
        } catch (Exception e) {
            log.warn("Could not parse password policy: {}", e.getMessage());
        }
        return defaultPolicy();
    }

    private static PasswordPolicyDto defaultPolicy() {
        return new PasswordPolicyDto(8, 100, false, false, false, false, 0, 0,
                "Geen specifiek beleid gevonden of niet via API beschikbaar.");
    }

    private TwoStepVerificationDto buildTwoStepVerification(String adminEmail, List<User> allUsers) {
        Map<String, String> ouIdToPath = Collections.emptyMap();
        try {
            ouIdToPath = policyCache.getOuIdToPathMap(adminEmail);
        } catch (Exception e) {
            log.warn("Could not fetch OU map: {}", e.getMessage());
        }

        Map<String, List<User>> byOu = allUsers.stream()
                .collect(Collectors.groupingBy(u -> {
                    String path = u.getOrgUnitPath();
                    return path != null && !path.isBlank() ? path.trim() : "/";
                }));

        List<OrgUnit2SvDto> ouList = new ArrayList<>();
        for (Map.Entry<String, List<User>> e : byOu.entrySet()) {
            String path = e.getKey();
            List<User> users = e.getValue();
            if (users.isEmpty()) continue;

            int total = users.size();
            int enrolled = (int) users.stream().filter(u -> Boolean.TRUE.equals(u.getIsEnrolledIn2Sv())).count();
            int enforced = (int) users.stream().filter(u -> Boolean.TRUE.equals(u.getIsEnforcedIn2Sv())).count();
            boolean enforcedByPolicy = is2SvEnforcedForOu(adminEmail, path, ouIdToPath);

            String displayName = path.equals("/") ? "Root" : path.substring(path.lastIndexOf('/') + 1);

            ouList.add(new OrgUnit2SvDto(path, displayName, enforcedByPolicy || enforced > 0, enrolled, total));
        }

        ouList.sort(Comparator.comparing(OrgUnit2SvDto::orgUnitPath));

        int totalEnrolled = (int) allUsers.stream().filter(u -> Boolean.TRUE.equals(u.getIsEnrolledIn2Sv())).count();
        int totalEnforced = (int) allUsers.stream().filter(u -> Boolean.TRUE.equals(u.getIsEnforcedIn2Sv())).count();

        return new TwoStepVerificationDto(ouList, totalEnrolled, totalEnforced, allUsers.size());
    }

    private boolean is2SvEnforcedForOu(String adminEmail, String orgUnitPath, Map<String, String> ouIdToPath) {
        try {
            List<JsonNode> policies = policyCache.getAllPolicies(adminEmail);
            for (JsonNode p : policies) {
                JsonNode setting = p.get("setting");
                if (setting == null) continue;
                String type = setting.path("type").asText("");
                if (!type.contains("two_step") && !type.contains("2sv") && !type.contains("two_step_verification")) continue;

                JsonNode query = p.get("policyQuery");
                if (query == null) continue;
                String ouRef = query.path("orgUnit").asText("");
                String policyOuPath = policyCache.resolveOuIdToPath(ouRef, ouIdToPath);
                if (isAncestorOrSelf(orgUnitPath, policyOuPath)) {
                    JsonNode value = setting.get("value");
                    if (value != null && value.path("enforced").asBoolean(false)) return true;
                }
            }
        } catch (Exception e) {
            log.warn("Could not check 2SV policy for OU {}: {}", orgUnitPath, e.getMessage());
        }
        return false;
    }

    private static boolean isAncestorOrSelf(String target, String policyOuPath) {
        if ("/".equals(policyOuPath)) return true;
        if (target.equals(policyOuPath)) return true;
        return target.startsWith(policyOuPath + "/");
    }

    private List<PasswordChangeRequirementDto> getUsersWithForcedPasswordChange(List<User> allUsers) {
        return allUsers.stream()
                .filter(u -> Boolean.TRUE.equals(u.getChangePasswordAtNextLogin()))
                .map(u -> new PasswordChangeRequirementDto(
                        u.getPrimaryEmail(),
                        u.getName() != null ? u.getName().getFullName() : u.getPrimaryEmail(),
                        u.getOrgUnitPath() != null ? u.getOrgUnitPath() : "/",
                        "changePasswordAtNextLogin"
                ))
                .collect(Collectors.toList());
    }
}
