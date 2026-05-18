package com.cloudmen.cloudguard.service;

import com.cloudmen.cloudguard.dto.adminsecuritykeys.AdminWithSecurityKeyDto;
import com.cloudmen.cloudguard.dto.password.*;
import com.cloudmen.cloudguard.service.cache.GoogleOrgUnitCacheService;
import com.cloudmen.cloudguard.service.cache.GoogleUsersCacheService;
import com.cloudmen.cloudguard.service.cache.PolicyApiCacheService;
import com.cloudmen.cloudguard.service.preference.UserSecurityPreferenceService;
import com.cloudmen.cloudguard.service.user.UserService;
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

import static com.cloudmen.cloudguard.utility.GoogleServiceHelperMethods.*;

/**
 * Builds the password-settings overview for a tenant: merges cached Directory users, org-unit paths,
 * Admin SDK policy payloads ({@code settings/security.password}, 2SV-related rules), and admin hardware-key coverage.
 * <p>
 * Responses are cached per logged-in user for one hour. Refresh explicitly invalidates this slice plus policy,
 * users, org-unit, and admin-keys caches.
 * </p>
 * <p>
 * The headline {@linkplain PasswordSettingsOverviewResponse#securityScore() security score} blends weighted factors:
 * admin keys (15%), forced password change rate (15%), OU-weighted 2SV stance (30%), minimum length (15%),
 * expiration (10%), strength (10%), and a neutral reuse-history slot (5%) for chart symmetry.
 * When {@code summary.totalUsers() == 0}, scores are omitted ({@code null}).
 * </p>
 *
 * @see PasswordSettingsController
 */
@Service
public class PasswordSettingsService {
    private static final Logger log = LoggerFactory.getLogger(PasswordSettingsService.class);

    private final PolicyApiCacheService policyCache;
    private final GoogleUsersCacheService usersCache;
    private final GoogleOrgUnitCacheService orgUnitCache;
    private final AdminSecurityKeysService adminSecurityKeysService;
    private final UserSecurityPreferenceService userSecurityPreferenceService;

    private final Cache<String, PasswordSettingsOverviewResponse> cache = Caffeine.newBuilder()
            .expireAfterWrite(1, TimeUnit.HOURS)
            .maximumSize(100)
            .build();
    private final MessageSource messageSource;
    private final UserService userService;
    private final OrganizationService organizationService;

    /**
     * @param policyCache                  Chrome / Workspace policy JSON and OU id→path maps
     * @param usersCache                   Directory users for the tenant
     * @param orgUnitCache                 OU tree and per-path user counts
     * @param adminSecurityKeysService     admins missing registered security keys (Reports-backed where applicable)
     * @param userSecurityPreferenceService injected for parity with preference-aware modules (reserved for future scoring tweaks)
     * @param messageSource                localized score factor copy
     * @param userService                  resolves impersonated admin email
     * @param organizationService          tenant lookup for admin resolution
     */
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

    /** Clears this service’s cache entry and forces upstream caches used by {@link #fetchPasswordSettings(String)} to reload. */
    public void forceRefreshCache(String loggedInEmail) {
        cache.invalidate(loggedInEmail);
        policyCache.forceRefreshCache(loggedInEmail);
        usersCache.forceRefreshCache(loggedInEmail);
        orgUnitCache.forceRefreshCache(loggedInEmail);
        adminSecurityKeysService.forceRefreshCache(loggedInEmail);
    }

    /**
     * Cached overview for {@code loggedInEmail}’s organization (impersonated Google admin under the hood).
     *
     * @param loggedInEmail authenticated workspace user email (cookie-derived upstream)
     */
    public PasswordSettingsOverviewResponse getPasswordSettings(String loggedInEmail) {
        return cache.get(loggedInEmail, this::fetchPasswordSettings);
    }

    /**
     * Assembles policies per OU, 2SV analytics, forced-change users, admin-key gaps, and weighted score in one pass.
     * Score and breakdown are {@code null} when there are no users ({@code summary.totalUsers() == 0}).
     */
    private PasswordSettingsOverviewResponse fetchPasswordSettings(String loggedInEmail) {
        String adminEmail = GoogleServiceHelperMethods.getAdminEmailForUser(loggedInEmail, userService, organizationService);

        var entry = usersCache.getOrFetchData(loggedInEmail);
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

        var scoreResult = calculateSecurityScoreWithBreakdown(
                adminSecurityKeysResponse.totalAdmins(),
                adminsWithoutSecurityKeys.size(),
                summary,
                twoStepVerification,
                passwordPoliciesByOu);

        if(summary.totalUsers()==0){
            return new PasswordSettingsOverviewResponse(passwordPoliciesByOu, twoStepVerification, forcedChange, summary,
                    adminsWithoutSecurityKeys, adminsSecurityKeysErrorMessage, null, null
            );
        }

        return new PasswordSettingsOverviewResponse(passwordPoliciesByOu, twoStepVerification, forcedChange, summary,
                adminsWithoutSecurityKeys, adminsSecurityKeysErrorMessage, scoreResult.score(), scoreResult.breakdown());
    }

    /**
     * One {@link OuPasswordPolicyDto} per known OU path (root plus cached org units), merging Policy API password rules.
     * Missing upstream data yields empty policy lists and best-effort defaults logged at warn level.
     */
    private List<OuPasswordPolicyDto> buildPasswordPoliciesPerOu(String loggedInEmail, String adminEmail) {
        Map<String, Integer> userCounts = new HashMap<>();
        List<String> ouPaths = new ArrayList<>();

        try {
            var ouEntry = orgUnitCache.getOrFetchData(loggedInEmail);
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

    /**
     * Picks the winning {@code settings/security.password} policy for {@code orgUnitPath} by deepest matching OU and
     * tie-breaking {@code sortOrder}, then maps JSON fields into {@link OuPasswordPolicyDto}.
     */
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

    /** Counts weak signals (weak strength, no expiry, no reuse prevention, short minimum length). */
    private static int countProblems(Integer minLength, Integer expirationDays, Boolean strongPassword, Integer reuseCount) {
        int p = 0;
        if (strongPassword != null && !strongPassword) p++;
        if (expirationDays != null && expirationDays == 0) p++;
        if (reuseCount != null && reuseCount == 0) p++;
        if (minLength != null && minLength < 8) p++;
        return p;
    }

    /** Parses Policy API duration strings such as {@code "2592000s"} into whole days (fractional seconds truncated). */
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

    /** Organizational depth used for “closest OU wins” policy resolution ({@code /} → {@code 0}). */
    private static int depthOf(String ouPath) {
        if (ouPath == null || "/".equals(ouPath)) return 0;
        int count = 0;
        for (char c : ouPath.toCharArray()) if (c == '/') count++;
        return count;
    }

    /** Groups users by OU path and complements Directory enrollment flags with policy-derived enforcement hints. */
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

    /**
     * {@code true} when any applicable policy node for {@code orgUnitPath} sets {@code enforced} under a type key that
     * looks like two-step verification ({@code two_step}, {@code 2sv}, etc.).
     */
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

    /** {@code true} when {@code policyOuPath} is root, equals {@code target}, or is a strict prefix path of {@code target}. */
    private static boolean isAncestorOrSelf(String target, String policyOuPath) {
        if ("/".equals(policyOuPath)) return true;
        if (target.equals(policyOuPath)) return true;
        return target.startsWith(policyOuPath + "/");
    }

    /** Users with Directory {@link User#getChangePasswordAtNextLogin()} {@code true}. */
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
     * Weighted headline score with localized factor rows for the UI.
     *
     * @param score     rounded 0–100 blend of admin keys, forced-change pressure, 2SV stance, length, expiry, strength, history slot
     * @param breakdown translated titles/descriptions and per-factor severities
     */
    public record SecurityScoreResult(int score, SecurityScoreBreakdownDto breakdown) {}

    /**
     * Computes the tenant-level score and breakdown using fixed weights:
     * <ul>
     *   <li>Admin security keys (15%): fraction of admins with keys registered</li>
     *   <li>Forced password change (15%): inverse of users with {@code changePasswordAtNextLogin}</li>
     *   <li>2-step verification (30%): OU-user weighted; {@code enforced → 100}, otherwise {@code 50}</li>
     *   <li>Minimum length (15%): bucket scores per OU user weights (≥14→100 down through &lt;8→0)</li>
     *   <li>Expiration (10%): {@code expirationDays == 0 → 0}, else 100 when defined per OU</li>
     *   <li>Strength (10%): strong required → 100 else 0, OU-user weighted</li>
     *   <li>Reuse history (5%): contributes neutral 100 to the weighted sum (placeholder factor)</li>
     * </ul>
     *
     * @param totalAdmins           admins considered for hardware-key coverage
     * @param adminsWithoutKeys     admins missing keys ({@link AdminSecurityKeysService} semantics)
     * @param summary               Directory rollups for forced change and totals
     * @param twoStepVerification   per-OU rows driving weighted 2SV stance
     * @param passwordPoliciesByOu  resolved OU policies after Policy API merge
     * @return composite score and chart-ready breakdown (via {@link SecurityScoreResult})
     */
    public SecurityScoreResult calculateSecurityScoreWithBreakdown(
            int totalAdmins,
            int adminsWithoutKeys,
            PasswordSettingsSummaryDto summary,
            TwoStepVerificationDto twoStepVerification,
            List<OuPasswordPolicyDto> passwordPoliciesByOu) {

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

        double wAdmin = 0.15;
        double wChange = 0.15;
        double w2fa = 0.30;
        double wLen = 0.15;
        double wExp = 0.10;
        double wStr = 0.10;
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

        int dispAdmin = (int) Math.round(adminKeysScore);
        int disp2fa = (int) Math.round(twoFaScore);
        int dispLen = (int) Math.round(lengthScore);
        int dispExp = (int) Math.round(expirationScore);
        int dispStr = (int) Math.round(strengthScore);

        boolean showAdmin = totalAdmins > 0;
        boolean showUserChange = summary.totalUsers() > 0;
        boolean show2fa = totalUsers2Fa > 0;
        boolean showLen = totalPolicyUsers > 0;
        boolean showExp = totalPolicyUsers > 0;
        boolean showStr = totalPolicyUsers > 0;

        List<SecurityScoreFactorDto> factors = List.of(
                securityScoreFactorForDetail(showAdmin, "Admin Security Keys", adminKeysDesc, dispAdmin, 100, severity(dispAdmin)),
                securityScoreFactorForDetail(
                        showUserChange,
                        messageSource.getMessage("password-settings.score.factor.users_change.title", null, locale),
                        usersNeedChangeDesc,
                        (int) Math.round(usersNeedChangeScore),
                        100,
                        severity(usersNeedChangeScore)),
                securityScoreFactorForDetail(show2fa, "2-Step Verification", twoFaDesc, disp2fa, 100, severity(disp2fa)),
                securityScoreFactorForDetail(
                        showLen,
                        messageSource.getMessage("password-settings.score.factor.length.title", null, locale),
                        lengthDesc,
                        dispLen,
                        100,
                        severity(dispLen)),
                securityScoreFactorForDetail(
                        showExp,
                        messageSource.getMessage("password-settings.score.factor.expiration.title", null, locale),
                        expirationDesc,
                        dispExp,
                        100,
                        severity(dispExp)),
                securityScoreFactorForDetail(
                        showStr,
                        messageSource.getMessage("password-settings.score.factor.strength.title", null, locale),
                        strengthDesc,
                        dispStr,
                        100,
                        severity(dispStr))
        );

        String status = getOverviewStatus(totalScore);

        return new SecurityScoreResult(totalScore, new SecurityScoreBreakdownDto(totalScore, status, factors));
    }
}
