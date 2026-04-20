package com.cloudmen.cloudguard.service;

import com.cloudmen.cloudguard.dto.adminsecuritykeys.AdminWithSecurityKeyDto;
import com.cloudmen.cloudguard.dto.password.*;
import com.cloudmen.cloudguard.service.cache.GoogleOrgUnitCacheService;
import com.cloudmen.cloudguard.service.cache.GoogleUsersCacheService;
import com.cloudmen.cloudguard.service.cache.PolicyApiCacheService;
import com.cloudmen.cloudguard.service.preference.SecurityPreferenceScoreSupport;
import com.cloudmen.cloudguard.service.preference.UserSecurityPreferenceService;
import com.cloudmen.cloudguard.utility.GoogleServiceHelperMethods;
import com.fasterxml.jackson.databind.JsonNode;
import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import com.google.api.services.admin.directory.model.OrgUnit;
import com.google.api.services.admin.directory.model.User;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.MessageSource;
import org.springframework.context.i18n.LocaleContextHolder;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

import static com.cloudmen.cloudguard.utility.GoogleServiceHelperMethods.securityScoreFactorForDetail;
import static com.cloudmen.cloudguard.utility.GoogleServiceHelperMethods.severity;

@Service
public class PasswordSettingsService {
    private static final Logger log = LoggerFactory.getLogger(PasswordSettingsService.class);

    private final PolicyApiCacheService policyCache;
    private final GoogleUsersCacheService usersCache;
    private final GoogleOrgUnitCacheService orgUnitCache;
    private final AdminSecurityKeysService adminSecurityKeysService;
    private final UserSecurityPreferenceService userSecurityPreferenceService;

    private final Cache<String, PasswordSettingsDto> cache = Caffeine.newBuilder()
            .expireAfterWrite(1, TimeUnit.HOURS)
            .maximumSize(100)
            .build();
    private final MessageSource messageSource;
    private final UserService userService;
    private final OrganizationService organizationService;

    public PasswordSettingsService(PolicyApiCacheService policyCache,
                                   GoogleUsersCacheService usersCache,
                                   GoogleOrgUnitCacheService orgUnitCache,
                                   AdminSecurityKeysService adminSecurityKeysService,
                                   UserSecurityPreferenceService userSecurityPreferenceService,
                                   MessageSource messageSource, UserService userService, OrganizationService organizationService) {
        this.policyCache = policyCache;
        this.usersCache = usersCache;
        this.orgUnitCache = orgUnitCache;
        this.adminSecurityKeysService = adminSecurityKeysService;
        this.userSecurityPreferenceService = userSecurityPreferenceService;
        this.messageSource = messageSource;
        this.userService = userService;
        this.organizationService = organizationService;
    }

    public void forceRefreshCache(String loggedInEmail) {
        cache.invalidate(loggedInEmail);
        policyCache.forceRefreshCache();
        usersCache.forceRefreshCache(loggedInEmail);
        orgUnitCache.forceRefreshCache(loggedInEmail);
        adminSecurityKeysService.forceRefreshCache(loggedInEmail);
    }

    public PasswordSettingsDto getPasswordSettings(String loggedInEmail) {
        return cache.get(loggedInEmail, this::fetchPasswordSettings);
    }

    private PasswordSettingsDto fetchPasswordSettings(String loggedInEmail) {
        String adminEmail = GoogleServiceHelperMethods.getAdminEmailForUser(loggedInEmail, userService, organizationService);

        var entry = usersCache.getOrFetchUsersData(loggedInEmail);
        List<User> allUsers = entry.allUsers();


        List<OuPasswordPolicyDto> passwordPoliciesByOu = buildPasswordPoliciesPerOu(loggedInEmail, adminEmail);
        TwoStepVerificationDto twoStepVerification = buildTwoStepVerification(adminEmail, allUsers);
        List<PasswordChangeRequirementDto> forcedChange = getUsersWithForcedPasswordChange(allUsers);

        int total = allUsers.size();
        int enrolled = (int) allUsers.stream().filter(u -> Boolean.TRUE.equals(u.getIsEnrolledIn2Sv())).count();
        int enforced = (int) allUsers.stream().filter(u -> Boolean.TRUE.equals(u.getIsEnforcedIn2Sv())).count();

        PasswordSettingsSummaryDto summary = new PasswordSettingsSummaryDto(
                forcedChange.size(), enrolled, enforced, total);

        var adminSecurityKeysResponse = adminSecurityKeysService.getAdminsWithSecurityKeys(loggedInEmail);
        List<AdminWithSecurityKeyDto> adminsWithoutSecurityKeys = adminSecurityKeysResponse.admins() != null
                ? adminSecurityKeysResponse.admins() : List.of();
        String adminsSecurityKeysErrorMessage = adminSecurityKeysResponse.errorMessage();

        Set<String> disabledPrefs = userSecurityPreferenceService.getDisabledPreferenceKeys(loggedInEmail);
        var scoreResult = calculateSecurityScoreWithBreakdown(
                adminSecurityKeysResponse.totalAdmins(),
                adminsWithoutSecurityKeys.size(),
                summary,
                twoStepVerification,
                passwordPoliciesByOu,
                disabledPrefs);

        return new PasswordSettingsDto(passwordPoliciesByOu, twoStepVerification, forcedChange, summary,
                adminsWithoutSecurityKeys, adminsSecurityKeysErrorMessage, scoreResult.score(), scoreResult.breakdown());
    }

    private List<OuPasswordPolicyDto> buildPasswordPoliciesPerOu(String loggedInEmail, String adminEmail) {
        Map<String, Integer> userCounts = new HashMap<>();
        List<String> ouPaths = new ArrayList<>();

        try {
            var ouEntry = orgUnitCache.getOrFetchOrgUnitData(loggedInEmail);
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

        List<JsonNode> policies = Collections.emptyList();
        try {
            policies = policyCache.getAllPolicies(adminEmail);
        } catch (Exception e) {
            log.warn("Could not fetch policies: {}", e.getMessage());
        }

        List<OuPasswordPolicyDto> result = new ArrayList<>();
        for (String path : ouPaths) {
            OuPasswordPolicyDto policy = resolvePasswordPolicyForOu(path, policies, ouIdToPath,
                    userCounts.getOrDefault(path, 0));
            result.add(policy);
        }
        result.sort(Comparator.comparing(OuPasswordPolicyDto::orgUnitPath));
        return result;
    }

    private OuPasswordPolicyDto resolvePasswordPolicyForOu(String orgUnitPath, List<JsonNode> policies, Map<String, String> ouIdToPath, int userCount) {
        String displayName = "/".equals(orgUnitPath) ? "Root" : orgUnitPath.substring(orgUnitPath.lastIndexOf('/') + 1);

        Integer minLength = null;
        Integer expirationDays = null;
        Boolean strongPassword = null;
        Integer reuseCount = null;
        boolean inherited = true;

        JsonNode best = null;
        String bestOuPath = null;
        int bestDepth = -1;
        double bestSort = Double.NEGATIVE_INFINITY;

        for (JsonNode p : policies) {
            JsonNode setting = p.get("setting");
            if (setting == null) continue;
            String type = setting.path("type").asText("");
            if (!"settings/security.password".equals(type)) continue;

            JsonNode query = p.get("policyQuery");
            if (query == null) continue;
            String ouRef = query.path("orgUnit").asText("");
            String policyOuPath = policyCache.resolveOuIdToPath(ouRef, ouIdToPath);
            if (!isAncestorOrSelf(orgUnitPath, policyOuPath)) continue;

            int depth = depthOf(policyOuPath);
            double sortOrder = query.path("sortOrder").asDouble(0.0);
            if (depth > bestDepth || (depth == bestDepth && sortOrder > bestSort)) {
                best = p;
                bestOuPath = policyOuPath;
                bestDepth = depth;
                bestSort = sortOrder;
            }
        }

        if (best != null) {
            log.info("OU [{}]: matched policy from OU [{}] (inherited={}). Raw value: {}",
                    orgUnitPath, bestOuPath, !orgUnitPath.equals(bestOuPath),
                    best.path("setting").path("value"));
            JsonNode value = best.path("setting").path("value");
            inherited = !orgUnitPath.equals(bestOuPath);

            JsonNode ml = value.path("minimumLength");
            if (!ml.isMissingNode() && !ml.isNull()) minLength = ml.asInt();

            JsonNode ed = value.path("expirationDuration");
            if (!ed.isMissingNode() && !ed.isNull()) {
                expirationDays = parseDurationToDays(ed.asText("0s"));
            }

            JsonNode strength = value.path("allowedStrength");
            if (!strength.isMissingNode() && !strength.isNull()) {
                strongPassword = "STRONG".equals(strength.asText());
            }

            JsonNode reuse = value.path("allowReuse");
            if (!reuse.isMissingNode() && !reuse.isNull()) {
                reuseCount = reuse.asBoolean() ? 0 : 1;
            }
        }

        int score = calculateScore(minLength, expirationDays, strongPassword, reuseCount);
        int problemCount = countProblems(minLength, expirationDays, strongPassword, reuseCount);

        return new OuPasswordPolicyDto(orgUnitPath, displayName, userCount, score, problemCount,
                minLength, expirationDays, strongPassword, reuseCount, inherited);
    }

    private static int calculateScore(Integer minLength, Integer expirationDays, Boolean strongPassword, Integer reuseCount) {
        int s = 0;
        if (Boolean.TRUE.equals(strongPassword)) s += 25;
        if (expirationDays != null && expirationDays > 0) s += 25;
        if (reuseCount != null && reuseCount > 0) s += 25;
        if (minLength != null) {
            if (minLength >= 12) s += 20;
            else if (minLength >= 8) s += 10;
        }
        return Math.min(100, s);
    }

    private static int countProblems(Integer minLength, Integer expirationDays, Boolean strongPassword, Integer reuseCount) {
        int p = 0;
        if (strongPassword != null && !strongPassword) p++;
        if (expirationDays != null && expirationDays == 0) p++;
        if (reuseCount != null && reuseCount == 0) p++;
        if (minLength != null && minLength < 8) p++;
        return p;
    }

    private static int parseDurationToDays(String duration) {
        if (duration == null || duration.isBlank()) return 0;
        try {
            String stripped = duration.replace("s", "");
            long seconds = Long.parseLong(stripped);
            return (int) (seconds / 86400);
        } catch (NumberFormatException e) {
            return 0;
        }
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

        List<JsonNode> policies = Collections.emptyList();
        try {
            policies = policyCache.getAllPolicies(adminEmail);
        } catch (Exception e) {
            log.warn("Could not fetch policies for 2SV: {}", e.getMessage());
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
            boolean enforcedByPolicy = is2SvEnforcedForOu(path, policies, ouIdToPath);

            String displayName = path.equals("/") ? "Root" : path.substring(path.lastIndexOf('/') + 1);

            ouList.add(new OrgUnit2SvDto(path, displayName, enforcedByPolicy || enforced > 0, enrolled, total));
        }

        ouList.sort(Comparator.comparing(OrgUnit2SvDto::orgUnitPath));

        int totalEnrolled = (int) allUsers.stream().filter(u -> Boolean.TRUE.equals(u.getIsEnrolledIn2Sv())).count();
        int totalEnforced = (int) allUsers.stream().filter(u -> Boolean.TRUE.equals(u.getIsEnforcedIn2Sv())).count();

        return new TwoStepVerificationDto(ouList, totalEnrolled, totalEnforced, allUsers.size());
    }

    private boolean is2SvEnforcedForOu(String orgUnitPath, List<JsonNode> policies, Map<String, String> ouIdToPath) {
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

    /**
     * Calculates weighted security score (0-100) based on:
     * - Admin security keys (15%): more admins with keys = better
     * - Users needing password change (15%): fewer = better
     * - 2FA enforcement (30%): per OU, weighted by users (enforced→100, optional→50, disabled→0)
     * - Password length (15%): ≥14→100, 12-13→90, 10-11→70, 8-9→40, <8→0
     * - Password expiration (10%): never expires→0, else→100 (null excluded)
     * - Password strength (10%): true→100, false→0
     * - Password history (5%): not used for score, always 100
     */
    record SecurityScoreResult(int score, SecurityScoreBreakdownDto breakdown) {}

    SecurityScoreResult calculateSecurityScoreWithBreakdown(
            int totalAdmins,
            int adminsWithoutKeys,
            PasswordSettingsSummaryDto summary,
            TwoStepVerificationDto twoStepVerification,
            List<OuPasswordPolicyDto> passwordPoliciesByOu,
            Set<String> disabledPrefs) {

        Set<String> off = disabledPrefs == null ? Set.of() : disabledPrefs;

        double adminKeysScore = totalAdmins == 0 ? 100.0
                : (totalAdmins - adminsWithoutKeys) * 100.0 / totalAdmins;

        double usersNeedChangeScore = summary.totalUsers() == 0 ? 100.0
                : Math.max(0, 100.0 - summary.usersWithForcedChange() * 100.0 / summary.totalUsers());

        double twoFaScore = 100.0;
        List<OrgUnit2SvDto> ou2Sv = twoStepVerification.byOrgUnit();
        int totalUsers2Fa = ou2Sv.stream().mapToInt(OrgUnit2SvDto::totalCount).sum();
        if (totalUsers2Fa > 0) {
            double weightedSum = 0;
            for (OrgUnit2SvDto ou : ou2Sv) {
                int score = ou.enforced() ? 100 : 50; // enforced→100, optional→50 (disabled not distinguished)
                weightedSum += score * ou.totalCount();
            }
            twoFaScore = weightedSum / totalUsers2Fa;
        }

        double lengthScore = 100.0;
        double expirationScore = 100.0;
        double strengthScore = 100.0;
        int totalPolicyUsers = passwordPoliciesByOu.stream().mapToInt(OuPasswordPolicyDto::userCount).sum();
        if (totalPolicyUsers > 0) {
            double lengthSum = 0, expirationSum = 0, strengthSum = 0;
            int totalExpirationUsers = 0;
            for (OuPasswordPolicyDto p : passwordPoliciesByOu) {
                int len = p.minLength() != null ? p.minLength() : 0;
                int lenScore = len >= 14 ? 100 : len >= 12 ? 90 : len >= 10 ? 70 : len >= 8 ? 40 : 0;
                lengthSum += lenScore * p.userCount();

                if (p.expirationDays() != null) {
                    int expScore = p.expirationDays() > 0 ? 100 : 0;
                    expirationSum += expScore * p.userCount();
                    totalExpirationUsers += p.userCount();
                }

                int strScore = Boolean.TRUE.equals(p.strongPasswordRequired()) ? 100 : 0;
                strengthSum += strScore * p.userCount();
            }
            lengthScore = lengthSum / totalPolicyUsers;
            expirationScore = totalExpirationUsers > 0 ? expirationSum / totalExpirationUsers : 100.0;
            strengthScore = strengthSum / totalPolicyUsers;
        }

        double historyScore = 100.0; // not used for security score

        double wAdmin = SecurityPreferenceScoreSupport.preferenceDisabled(off, "password-settings", "adminsSecurityKeys") ? 0 : 0.15;
        double wChange = 0.15;
        double w2fa = SecurityPreferenceScoreSupport.preferenceDisabled(off, "password-settings", "2sv") ? 0 : 0.30;
        double wLen = SecurityPreferenceScoreSupport.preferenceDisabled(off, "password-settings", "length") ? 0 : 0.15;
        double wExp = SecurityPreferenceScoreSupport.preferenceDisabled(off, "password-settings", "expiration") ? 0 : 0.10;
        double wStr = SecurityPreferenceScoreSupport.preferenceDisabled(off, "password-settings", "strongPassword") ? 0 : 0.10;
        double wHist = 0.05;

        double weightSum = wAdmin + wChange + w2fa + wLen + wExp + wStr + wHist;
        double weighted = weightSum <= 0 ? 100.0
                : (adminKeysScore * wAdmin
                + usersNeedChangeScore * wChange
                + twoFaScore * w2fa
                + lengthScore * wLen
                + expirationScore * wExp
                + strengthScore * wStr
                + historyScore * wHist) / weightSum;

        int totalScore = (int) Math.round(Math.max(0, Math.min(100, weighted)));

        Locale locale = LocaleContextHolder.getLocale();

        String adminKeysDesc = totalAdmins == 0 ? messageSource.getMessage("password-settings.score.factor.admin_keys.description.no_admins", null, locale)
                : adminsWithoutKeys == 0 ? messageSource.getMessage("password-settings.score.factor.admin_keys.description.admins_with_keys", null, locale)
                : messageSource.getMessage("password-settings.score.factor.admin_keys.description.admins_without_keys", new Object[]{adminsWithoutKeys, totalAdmins}, locale);

        String usersNeedChangeDesc = summary.totalUsers() == 0 ? messageSource.getMessage("password-settings.score.factor.users_change.description.no_users", null, locale)
                : summary.usersWithForcedChange() == 0 ? messageSource.getMessage("password-settings.score.factor.users_change.description.no_force_change", null, locale)
                : messageSource.getMessage("password-settings.score.factor.users_change.description.force_change", new Object[]{summary.usersWithForcedChange()}, locale);

        String twoFaDesc = totalUsers2Fa == 0 ? messageSource.getMessage("password-settings.score.factor.users_change.description.no_users", null, locale)
                : (int) twoFaScore == 100 ? messageSource.getMessage("password-settings.score.factor.two_FA.description.full", null, locale)
                : messageSource.getMessage("password-settings.score.factor.two_FA.description.not_full", null, locale);

        String lengthDesc = totalPolicyUsers == 0 ? messageSource.getMessage("password-settings.score.factor.description.no_OU", null, locale)
                : (int) lengthScore >= 90 ? messageSource.getMessage("password-settings.score.factor.length.description.full", null, locale)
                : messageSource.getMessage("password-settings.score.factor.length.description.not_full", null, locale);

        String expirationDesc = totalPolicyUsers == 0 ? messageSource.getMessage("password-settings.score.factor.description.no_OU", null, locale)
                : (int) expirationScore == 100 ? messageSource.getMessage("password-settings.score.factor.expiration.description.full", null, locale)
                : messageSource.getMessage("password-settings.score.factor.expiration.description.not_full", null, locale);

        String strengthDesc = totalPolicyUsers == 0 ? messageSource.getMessage("password-settings.score.factor.description.no_OU", null, locale)
                : (int) strengthScore == 100 ? messageSource.getMessage("password-settings.score.factor.strength.description.full", null, locale)
                : messageSource.getMessage("password-settings.score.factor.strength.description.not_full", null, locale);

        boolean muteAdmin = SecurityPreferenceScoreSupport.preferenceDisabled(off, "password-settings", "adminsSecurityKeys");
        boolean mute2sv = SecurityPreferenceScoreSupport.preferenceDisabled(off, "password-settings", "2sv");
        boolean muteLen = SecurityPreferenceScoreSupport.preferenceDisabled(off, "password-settings", "length");
        boolean muteExp = SecurityPreferenceScoreSupport.preferenceDisabled(off, "password-settings", "expiration");
        boolean muteStr = SecurityPreferenceScoreSupport.preferenceDisabled(off, "password-settings", "strongPassword");

        int dispAdmin = muteAdmin ? 100 : (int) Math.round(adminKeysScore);
        int disp2fa = mute2sv ? 100 : (int) Math.round(twoFaScore);
        int dispLen = muteLen ? 100 : (int) Math.round(lengthScore);
        int dispExp = muteExp ? 100 : (int) Math.round(expirationScore);
        int dispStr = muteStr ? 100 : (int) Math.round(strengthScore);

        boolean showAdmin = totalAdmins > 0 || muteAdmin;
        boolean showUserChange = summary.totalUsers() > 0;
        boolean show2fa = totalUsers2Fa > 0 || mute2sv;
        boolean showLen = totalPolicyUsers > 0 || muteLen;
        boolean showExp = totalPolicyUsers > 0 || muteExp;
        boolean showStr = totalPolicyUsers > 0 || muteStr;

        List<SecurityScoreFactorDto> factors = List.of(
                securityScoreFactorForDetail(showAdmin, "Admin Security Keys", adminKeysDesc, dispAdmin, 100, severity(dispAdmin), muteAdmin),
                securityScoreFactorForDetail(
                        showUserChange,
                        messageSource.getMessage("password-settings.score.factor.users_change.title", null, locale),
                        usersNeedChangeDesc,
                        (int) Math.round(usersNeedChangeScore),
                        100,
                        severity(usersNeedChangeScore),
                        false),
                securityScoreFactorForDetail(show2fa, "2-Step Verification", twoFaDesc, disp2fa, 100, severity(disp2fa), mute2sv),
                securityScoreFactorForDetail(
                        showLen,
                        messageSource.getMessage("password-settings.score.factor.length.title", null, locale),
                        lengthDesc,
                        dispLen,
                        100,
                        severity(dispLen),
                        muteLen),
                securityScoreFactorForDetail(
                        showExp,
                        messageSource.getMessage("password-settings.score.factor.expiration.title", null, locale),
                        expirationDesc,
                        dispExp,
                        100,
                        severity(dispExp),
                        muteExp),
                securityScoreFactorForDetail(
                        showStr,
                        messageSource.getMessage("password-settings.score.factor.strength.title", null, locale),
                        strengthDesc,
                        dispStr,
                        100,
                        severity(dispStr),
                        muteStr)
        );

        String status = totalScore == 100 ? "perfect" : totalScore >= 75 ? "good" : totalScore > 50 ? "average" : "bad";

        return new SecurityScoreResult(totalScore, new SecurityScoreBreakdownDto(totalScore, status, factors));
    }
}
