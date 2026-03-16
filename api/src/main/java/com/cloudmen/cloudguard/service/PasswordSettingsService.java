package com.cloudmen.cloudguard.service;

import com.cloudmen.cloudguard.dto.password.*;
import com.cloudmen.cloudguard.service.cache.GoogleOrgUnitCacheService;
import com.cloudmen.cloudguard.service.cache.GoogleUsersCacheService;
import com.cloudmen.cloudguard.service.cache.PolicyApiCacheService;
import com.fasterxml.jackson.databind.JsonNode;
import com.google.api.services.admin.directory.model.OrgUnit;
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
    private final GoogleOrgUnitCacheService orgUnitCache;

    public PasswordSettingsService(PolicyApiCacheService policyCache,
                                  GoogleUsersCacheService usersCache,
                                  GoogleOrgUnitCacheService orgUnitCache) {
        this.policyCache = policyCache;
        this.usersCache = usersCache;
        this.orgUnitCache = orgUnitCache;
    }

    public PasswordSettingsDto getPasswordSettings(String adminEmail) {
        var entry = usersCache.getOrFetchUsersData(adminEmail);
        List<User> allUsers = entry.allUsers();

        List<OuPasswordPolicyDto> passwordPoliciesByOu = buildPasswordPoliciesPerOu(adminEmail);
        TwoStepVerificationDto twoStepVerification = buildTwoStepVerification(adminEmail, allUsers);
        List<PasswordChangeRequirementDto> forcedChange = getUsersWithForcedPasswordChange(allUsers);

        int total = allUsers.size();
        int enrolled = (int) allUsers.stream().filter(u -> Boolean.TRUE.equals(u.getIsEnrolledIn2Sv())).count();
        int enforced = (int) allUsers.stream().filter(u -> Boolean.TRUE.equals(u.getIsEnforcedIn2Sv())).count();

        PasswordSettingsSummaryDto summary = new PasswordSettingsSummaryDto(
                forcedChange.size(), enrolled, enforced, total);

        return new PasswordSettingsDto(passwordPoliciesByOu, twoStepVerification, forcedChange, summary);
    }

    private List<OuPasswordPolicyDto> buildPasswordPoliciesPerOu(String adminEmail) {
        Map<String, Integer> userCounts = new HashMap<>();
        List<String> ouPaths = new ArrayList<>();
        try {
            var ouEntry = orgUnitCache.getOrFetchOrgUnitData(adminEmail);
            userCounts.putAll(ouEntry.userCounts());
            Set<String> pathSet = new LinkedHashSet<>();
            pathSet.add("/");
            for (OrgUnit ou : ouEntry.allOrgUnits()) {
                String path = ou.getOrgUnitPath();
                if (path != null && !path.isBlank()) {
                    pathSet.add(path.trim());
                }
            }
            ouPaths.addAll(pathSet);
        } catch (Exception e) {
            log.warn("Could not fetch OUs: {}", e.getMessage());
        }

        Map<String, String> ouIdToPath = Collections.emptyMap();
        try {
            ouIdToPath = policyCache.getOuIdToPathMap(adminEmail);
        } catch (Exception e) {
            log.warn("Could not fetch OU map: {}", e.getMessage());
        }

        List<OuPasswordPolicyDto> result = new ArrayList<>();
        for (String path : ouPaths) {
            OuPasswordPolicyDto policy = resolvePasswordPolicyForOu(adminEmail, path, ouIdToPath,
                    userCounts.getOrDefault(path, 0));
            result.add(policy);
        }
        result.sort(Comparator.comparing(OuPasswordPolicyDto::orgUnitPath));
        return result;
    }

    private OuPasswordPolicyDto resolvePasswordPolicyForOu(String adminEmail, String orgUnitPath, Map<String, String> ouIdToPath, int userCount) {
        String displayName = "/".equals(orgUnitPath) ? "Root" : orgUnitPath.substring(orgUnitPath.lastIndexOf('/') + 1);

        int minLength = 8;
        int expirationDays = 0;
        boolean strongPassword = false;
        boolean blockCommon = false;
        int reuseCount = 0;
        boolean inherited = true;
        boolean securityKeyRequired = false;
        boolean adminStrongPasswordEnforced = false;
        int adminMinLength = 8;

        try {
            List<JsonNode> policies = policyCache.getAllPolicies(adminEmail);
            JsonNode best = null;
            String bestOuPath = null;
            int bestDepth = -1;

            for (JsonNode p : policies) {
                JsonNode setting = p.get("setting");
                if (setting == null) continue;
                String type = setting.path("type").asText("");
                if (!type.contains("password")) continue;

                JsonNode query = p.get("policyQuery");
                if (query == null) continue;
                String ouRef = query.path("orgUnit").asText("");
                String policyOuPath = policyCache.resolveOuIdToPath(ouRef, ouIdToPath);
                if (!isAncestorOrSelf(orgUnitPath, policyOuPath)) continue;

                int depth = depthOf(policyOuPath);
                if (depth > bestDepth) {
                    best = p;
                    bestOuPath = policyOuPath;
                    bestDepth = depth;
                }
            }

            if (best != null) {
                JsonNode value = best.path("setting").path("value");
                boolean reqLower = value.path("requireLowercase").asBoolean(false);
                boolean reqUpper = value.path("requireUppercase").asBoolean(false);
                boolean reqNumeric = value.path("requireNumeric").asBoolean(false);
                boolean reqSpecial = value.path("requireSpecialChar").asBoolean(false);
                strongPassword = reqLower || reqUpper || reqNumeric || reqSpecial
                        || value.path("strongPasswordRequired").asBoolean(false);
                minLength = value.path("minLength").asInt(8);
                expirationDays = value.path("expirationDays").asInt(0);
                blockCommon = value.path("blockCommonPasswords").asBoolean(false);
                reuseCount = value.path("reusePreventionCount").asInt(0);
                inherited = !orgUnitPath.equals(bestOuPath);
                securityKeyRequired = value.path("securityKeyRequired").asBoolean(false);
                adminStrongPasswordEnforced = value.path("adminStrongPasswordEnforced").asBoolean(false)
                        || value.path("enforceStrongPasswordForAdmin").asBoolean(false);
                int adminMin = value.path("adminMinLength").asInt(-1);
                adminMinLength = adminMin >= 0 ? adminMin : value.path("minLengthForAdmin").asInt(minLength);
            }
        } catch (Exception e) {
            log.warn("Could not resolve password policy for OU {}: {}", orgUnitPath, e.getMessage());
        }

        int score = calculateScore(minLength, expirationDays, strongPassword, blockCommon, reuseCount);
        int problemCount = countProblems(minLength, expirationDays, strongPassword, blockCommon, reuseCount);

        return new OuPasswordPolicyDto(orgUnitPath, displayName, userCount, score, problemCount,
                minLength, expirationDays, strongPassword, blockCommon, reuseCount, inherited,
                securityKeyRequired, adminStrongPasswordEnforced, adminMinLength);
    }

    private static int calculateScore(int minLength, int expirationDays, boolean strongPassword, boolean blockCommon, int reuseCount) {
        int s = 0;
        if (strongPassword) s += 25;
        if (blockCommon) s += 25;
        if (expirationDays > 0) s += 25;
        if (reuseCount > 0) s += 25;
        if (minLength >= 12) s += 20;
        else if (minLength >= 8) s += 10;
        return Math.min(100, s);
    }

    private static int countProblems(int minLength, int expirationDays, boolean strongPassword, boolean blockCommon, int reuseCount) {
        int p = 0;
        if (!strongPassword) p++;
        if (!blockCommon) p++;
        if (expirationDays == 0) p++;
        if (reuseCount == 0) p++;
        if (minLength < 8) p++;
        return p;
    }

    private static int depthOf(String ouPath) {
        if (ouPath == null || "/".equals(ouPath)) return 0;
        int count = 0;
        for (char c : ouPath.toCharArray()) if (c == '/') count++;
        return count;
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
