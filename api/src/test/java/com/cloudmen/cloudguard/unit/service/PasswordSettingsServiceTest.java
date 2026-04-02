package com.cloudmen.cloudguard.service;

import com.cloudmen.cloudguard.dto.adminsecuritykeys.AdminSecurityKeysResponse;
import com.cloudmen.cloudguard.dto.adminsecuritykeys.AdminWithSecurityKeyDto;
import com.cloudmen.cloudguard.dto.organization.OrgUnitCacheEntry;
import com.cloudmen.cloudguard.dto.password.*;
import com.cloudmen.cloudguard.dto.users.UserCacheEntry;
import com.cloudmen.cloudguard.service.cache.GoogleOrgUnitCacheService;
import com.cloudmen.cloudguard.service.cache.GoogleUsersCacheService;
import com.cloudmen.cloudguard.service.cache.PolicyApiCacheService;
import com.cloudmen.cloudguard.service.preference.UserSecurityPreferenceService;
import com.google.api.services.admin.directory.model.User;
import com.google.api.services.admin.directory.model.UserName;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.support.ResourceBundleMessageSource;
import org.springframework.context.i18n.LocaleContextHolder;

import java.nio.charset.StandardCharsets;
import java.util.*;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class PasswordSettingsServiceTest {

    private static final String ADMIN = "admin@example.com";

    @Mock
    PolicyApiCacheService policyCache;
    @Mock
    GoogleUsersCacheService usersCache;
    @Mock
    GoogleOrgUnitCacheService orgUnitCache;
    @Mock
    AdminSecurityKeysService adminSecurityKeysService;
    @Mock
    UserSecurityPreferenceService userSecurityPreferenceService;

    private ResourceBundleMessageSource messageSource;
    private PasswordSettingsService service;

    @BeforeEach
    void setUp() {
        messageSource = new ResourceBundleMessageSource();
        messageSource.setBasenames("messages");
        messageSource.setDefaultEncoding(StandardCharsets.UTF_8.name());
        messageSource.setFallbackToSystemLocale(false);
        LocaleContextHolder.setLocale(Locale.ENGLISH);
        service = new PasswordSettingsService(
                policyCache, usersCache, orgUnitCache, adminSecurityKeysService, userSecurityPreferenceService, messageSource);
    }

    @AfterEach
    void tearDown() {
        LocaleContextHolder.resetLocaleContext();
    }

    @Test
    void forceRefreshCache_invalidatesInternalCacheAndRefreshesDependencies() {
        service.forceRefreshCache(ADMIN);

        verify(policyCache).forceRefreshCache();
        verify(usersCache).forceRefreshCache(ADMIN);
        verify(orgUnitCache).forceRefreshCache(ADMIN);
        verify(adminSecurityKeysService).forceRefreshCache(ADMIN);
    }

    @Test
    void getPasswordSettings_secondCallDoesNotRefetchUsersFromCacheService() throws Exception {
        stubMinimalTenant();

        when(adminSecurityKeysService.getAdminsWithSecurityKeys(ADMIN))
                .thenReturn(new AdminSecurityKeysResponse(Collections.emptyList(), 0));
        when(userSecurityPreferenceService.getDisabledPreferenceKeys(ADMIN)).thenReturn(Set.of());

        service.getPasswordSettings(ADMIN);
        service.getPasswordSettings(ADMIN);

        verify(usersCache, times(1)).getOrFetchUsersData(ADMIN);
    }

    @Test
    void getPasswordSettings_emptyUsers_summaryAndScoreStillCoherent() throws Exception {
        stubMinimalTenant();

        when(adminSecurityKeysService.getAdminsWithSecurityKeys(ADMIN))
                .thenReturn(new AdminSecurityKeysResponse(Collections.emptyList(), 0));
        when(userSecurityPreferenceService.getDisabledPreferenceKeys(ADMIN)).thenReturn(Set.of());

        PasswordSettingsDto dto = service.getPasswordSettings(ADMIN);

        assertEquals(0, dto.summary().totalUsers());
        assertEquals(100, dto.securityScore());
        assertEquals("perfect", dto.securityScoreBreakdown().status());
    }

    @Test
    void getPasswordSettings_wiresAdminKeysAndForcedPasswordChangeIntoDtoAndScore() throws Exception {
        User forced = new User();
        forced.setPrimaryEmail("alice@example.com");
        forced.setName(new UserName().setFullName("Alice"));
        forced.setOrgUnitPath("/");
        forced.setChangePasswordAtNextLogin(true);
        forced.setIsEnrolledIn2Sv(true);
        forced.setIsEnforcedIn2Sv(true);

        User ok = new User();
        ok.setPrimaryEmail("bob@example.com");
        ok.setName(new UserName().setFullName("Bob"));
        ok.setOrgUnitPath("/");
        ok.setChangePasswordAtNextLogin(false);
        ok.setIsEnrolledIn2Sv(true);
        ok.setIsEnforcedIn2Sv(true);

        when(usersCache.getOrFetchUsersData(ADMIN)).thenReturn(
                new UserCacheEntry(List.of(forced, ok), Map.of(), Map.of(), 0L));
        when(orgUnitCache.getOrFetchOrgUnitData(ADMIN)).thenReturn(
                new OrgUnitCacheEntry(List.of(), Map.of("/", 2), 0L));
        when(policyCache.getOuIdToPathMap(ADMIN)).thenReturn(Collections.emptyMap());
        when(policyCache.getAllPolicies(ADMIN)).thenReturn(Collections.emptyList());

        var missingKeyAdmin = new AdminWithSecurityKeyDto(
                "a1", "Admin One", "admin1@example.com", "Admin", "/", true, 0);
        when(adminSecurityKeysService.getAdminsWithSecurityKeys(ADMIN))
                .thenReturn(new AdminSecurityKeysResponse(List.of(missingKeyAdmin), 2, null));
        when(userSecurityPreferenceService.getDisabledPreferenceKeys(ADMIN)).thenReturn(Set.of());

        PasswordSettingsDto dto = service.getPasswordSettings(ADMIN);

        assertEquals(1, dto.usersWithForcedChange().size());
        assertEquals(1, dto.summary().usersWithForcedChange());
        assertEquals(2, dto.summary().totalUsers());
        assertEquals(1, dto.adminsWithoutSecurityKeys().size());
        assertEquals("admin1@example.com", dto.adminsWithoutSecurityKeys().get(0).email());
        assertNull(dto.adminsSecurityKeysErrorMessage());

        var breakdown = dto.securityScoreBreakdown();
        assertEquals(50, breakdown.factors().get(0).score());
        assertEquals(50, breakdown.factors().get(1).score());
        assertFalse(breakdown.factors().get(0).muted());
        assertFalse(breakdown.factors().get(1).muted());
        assertTrue(dto.securityScore() < 100);
    }

    @Test
    void calculateSecurityScore_allFactorsPerfect_returns100() {
        var summary = new PasswordSettingsSummaryDto(0, 10, 10, 10);
        var twoSv = new TwoStepVerificationDto(
                List.of(new OrgUnit2SvDto("/", "Root", true, 10, 10)),
                10, 10, 10);
        var policies = List.of(ouPolicy("/", 10, 14, 180, true));

        var result = service.calculateSecurityScoreWithBreakdown(
                2, 0, summary, twoSv, policies, Set.of());

        assertEquals(100, result.score());
        assertEquals("perfect", result.breakdown().status());
    }

    @Test
    void calculateSecurityScore_optional2SvLowersScore() {
        var summary = new PasswordSettingsSummaryDto(0, 5, 0, 10);
        var twoSv = new TwoStepVerificationDto(
                List.of(new OrgUnit2SvDto("/", "Root", false, 5, 10)),
                5, 0, 10);
        var policies = List.of(ouPolicy("/", 10, 14, 180, true));

        var result = service.calculateSecurityScoreWithBreakdown(
                1, 0, summary, twoSv, policies, Set.of());

        assertEquals(85, result.score());
        assertEquals(50, result.breakdown().factors().get(2).score());
        assertFalse(result.breakdown().factors().get(2).muted());
    }

    @Test
    void calculateSecurityScore_2svPreferenceDisabled_excludes2SvFromWeighting() {
        var summary = new PasswordSettingsSummaryDto(0, 5, 0, 10);
        var twoSv = new TwoStepVerificationDto(
                List.of(new OrgUnit2SvDto("/", "Root", false, 5, 10)),
                5, 0, 10);
        var policies = List.of(ouPolicy("/", 10, 14, 180, true));

        var result = service.calculateSecurityScoreWithBreakdown(
                1, 0, summary, twoSv, policies,
                Set.of("password-settings:2sv"));

        assertEquals(100, result.score());
        assertTrue(result.breakdown().factors().get(2).muted());
        assertEquals(100, result.breakdown().factors().get(2).score());
    }

    @Test
    void calculateSecurityScore_adminsKeysPreferenceMuted_renormalizesAndShowsMutedFactorAt100() {
        var summary = new PasswordSettingsSummaryDto(0, 10, 10, 10);
        var twoSv = new TwoStepVerificationDto(
                List.of(new OrgUnit2SvDto("/", "Root", true, 10, 10)),
                0, 10, 10);
        var policies = List.of(ouPolicy("/", 10, 14, 180, true));

        var withoutMute = service.calculateSecurityScoreWithBreakdown(
                2, 1, summary, twoSv, policies, Set.of());
        assertEquals(93, withoutMute.score());

        var muted = service.calculateSecurityScoreWithBreakdown(
                2, 1, summary, twoSv, policies,
                Set.of("password-settings:adminsSecurityKeys"));

        var adminFactor = muted.breakdown().factors().get(0);
        assertTrue(adminFactor.muted());
        assertEquals(100, adminFactor.score());
        assertEquals(100, muted.score());
    }

    @Test
    void calculateSecurityScore_noAdmins_adminKeysFactorTreatedAsPerfect() {
        var summary = new PasswordSettingsSummaryDto(0, 0, 0, 0);
        var twoSv = new TwoStepVerificationDto(List.of(), 0, 0, 0);

        var result = service.calculateSecurityScoreWithBreakdown(
                0, 0, summary, twoSv, List.of(), Set.of());

        assertEquals(100, result.breakdown().factors().get(0).score());
    }

    @Test
    void calculateSecurityScore_nullDisabledPrefs_treatedAsEmpty() {
        var summary = new PasswordSettingsSummaryDto(0, 10, 10, 10);
        var twoSv = new TwoStepVerificationDto(
                List.of(new OrgUnit2SvDto("/", "Root", true, 10, 10)),
                10, 10, 10);
        var policies = List.of(ouPolicy("/", 10, 14, 180, true));

        var result = service.calculateSecurityScoreWithBreakdown(
                2, 0, summary, twoSv, policies, null);

        assertEquals(100, result.score());
    }

    @Test
    void calculateSecurityScore_mutesPolicyFactorsWhenPreferencesDisabled() {
        var summary = new PasswordSettingsSummaryDto(0, 0, 0, 10);
        var twoSv = new TwoStepVerificationDto(
                List.of(new OrgUnit2SvDto("/", "Root", true, 10, 10)),
                0, 10, 10);
        var policies = List.of(ouPolicy("/", 10, 8, 0, false));

        var result = service.calculateSecurityScoreWithBreakdown(
                1, 0, summary, twoSv, policies,
                Set.of("password-settings:length", "password-settings:expiration", "password-settings:strongPassword"));

        var factors = result.breakdown().factors();
        assertTrue(factors.get(3).muted());
        assertTrue(factors.get(4).muted());
        assertTrue(factors.get(5).muted());
        assertEquals(100, factors.get(3).score());
        assertEquals(100, factors.get(4).score());
        assertEquals(100, factors.get(5).score());
    }

    @Test
    void calculateSecurityScore_usersForcedToChangePassword_lowersScore() {
        var summary = new PasswordSettingsSummaryDto(5, 10, 10, 10);
        var twoSv = new TwoStepVerificationDto(
                List.of(new OrgUnit2SvDto("/", "Root", true, 10, 10)),
                10, 10, 10);
        var policies = List.of(ouPolicy("/", 10, 14, 180, true));

        var result = service.calculateSecurityScoreWithBreakdown(
                2, 0, summary, twoSv, policies, Set.of());

        assertEquals(50, result.breakdown().factors().get(1).score());
        assertTrue(result.score() < 100);
    }

    @Test
    void calculateSecurityScore_noForcedPasswordChanges_usersChangeFactorAt100() {
        var summary = new PasswordSettingsSummaryDto(0, 10, 10, 10);
        var twoSv = new TwoStepVerificationDto(
                List.of(new OrgUnit2SvDto("/", "Root", true, 10, 10)),
                10, 10, 10);
        var policies = List.of(ouPolicy("/", 10, 14, 180, true));

        var result = service.calculateSecurityScoreWithBreakdown(
                2, 0, summary, twoSv, policies, Set.of());

        assertEquals(100, result.breakdown().factors().get(1).score());
        assertFalse(result.breakdown().factors().get(1).muted());
    }

    @Test
    void calculateSecurityScore_partialAdminKeysWithoutSecurityKeys_lowersAdminFactor() {
        var summary = new PasswordSettingsSummaryDto(0, 10, 10, 10);
        var twoSv = new TwoStepVerificationDto(
                List.of(new OrgUnit2SvDto("/", "Root", true, 10, 10)),
                10, 10, 10);
        var policies = List.of(ouPolicy("/", 10, 14, 180, true));

        var result = service.calculateSecurityScoreWithBreakdown(
                4, 2, summary, twoSv, policies, Set.of());

        assertEquals(50, result.breakdown().factors().get(0).score());
        assertFalse(result.breakdown().factors().get(0).muted());
        assertTrue(result.score() < 100);
    }

    private void stubMinimalTenant() throws Exception {
        when(usersCache.getOrFetchUsersData(ADMIN)).thenReturn(
                new UserCacheEntry(List.of(), Map.of(), Map.of(), 0L));
        when(orgUnitCache.getOrFetchOrgUnitData(ADMIN)).thenReturn(
                new OrgUnitCacheEntry(List.of(), Map.of("/", 0), 0L));
        when(policyCache.getOuIdToPathMap(ADMIN)).thenReturn(Collections.emptyMap());
        when(policyCache.getAllPolicies(ADMIN)).thenReturn(Collections.emptyList());
    }

    private static OuPasswordPolicyDto ouPolicy(
            String path, int userCount, int minLen, int expDays, boolean strong) {
        return new OuPasswordPolicyDto(
                path, path, userCount, 0, 0, minLen, expDays, strong, 0, false);
    }
}
