package com.cloudmen.cloudguard.unit.service.preference;

import com.cloudmen.cloudguard.domain.model.User;
import com.cloudmen.cloudguard.domain.model.preference.UserSecurityPreference;
import com.cloudmen.cloudguard.exception.SecurityPreferenceValidationException;
import com.cloudmen.cloudguard.repository.UserRepository;
import com.cloudmen.cloudguard.repository.UserSecurityPreferenceRepository;
import com.cloudmen.cloudguard.service.preference.UserSecurityPreferenceService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.MessageSource;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class UserSecurityPreferenceServiceTest {

    private static final long ORG_ID = 99L;
    private static final String USER_EMAIL = "u1";

    @Mock
    UserSecurityPreferenceRepository repository;

    @Mock
    UserRepository userRepository;

    @Mock
    MessageSource messageSource;

    UserSecurityPreferenceService service;

    @BeforeEach
    void setUp() {
        service = new UserSecurityPreferenceService(repository, userRepository, messageSource);
        User u = new User();
        u.setEmail(USER_EMAIL);
        u.setOrganizationId(ORG_ID);
        lenient().when(userRepository.findByEmail(USER_EMAIL)).thenReturn(Optional.of(u));
    }

    @Test
    void getDisabledPreferenceKeys_onlyDisabledRows() {
        UserSecurityPreference on = row("u1", "users-groups", "2fa", true, null);
        UserSecurityPreference off = row("u1", "users-groups", "activity", false, null);
        when(repository.findByOrganizationId(ORG_ID)).thenReturn(List.of(on, off));

        Set<String> disabled = service.getDisabledPreferenceKeys(USER_EMAIL);
        assertEquals(Set.of("users-groups:activity"), disabled);
    }

    @Test
    void effectiveDnsImportanceDisplay_mergesOverridesWithDefaults() {
        UserSecurityPreference spfOverride = row("u1", "domain-dns", "impSpf", true, "OPTIONAL");
        when(repository.findByOrganizationIdAndSection(ORG_ID, "domain-dns")).thenReturn(List.of(spfOverride));

        Map<String, String> display = service.effectiveDnsImportanceDisplay(USER_EMAIL);
        assertEquals("OPTIONAL", display.get("SPF"));
        assertEquals("REQUIRED", display.get("MX"));
        assertEquals("OPTIONAL", display.get("TXT"));
    }

    @Test
    void getPreferencesForSection_skipsDnsImpKeys() {
        when(repository.findByOrganizationIdAndSection(ORG_ID, "domain-dns")).thenReturn(List.of(
                row("u1", "domain-dns", "impSpf", true, "REQUIRED"),
                row("u1", "domain-dns", "someToggle", false, null)
        ));

        Map<String, Boolean> section = service.getPreferencesForSection(USER_EMAIL, "domain-dns");
        assertEquals(Map.of("someToggle", false), section);
    }

    @Test
    void isNotificationHiddenByPreference_tracksDisabledMapping() {
        when(repository.findByOrganizationId(ORG_ID)).thenReturn(List.of(
                row("u1", "password-settings", "2sv", false, null)
        ));

        assertTrue(service.isNotificationHiddenByPreference(
                USER_EMAIL, "password-settings", "password-2sv-not-enforced"));
        assertFalse(service.isNotificationHiddenByPreference(
                USER_EMAIL, "password-settings", "password-weak-length"));
    }

    @Test
    void setPreference_dnsBlankValue_deletesRow() {
        when(repository.findByOrganizationIdAndSectionAndPreferenceKey(ORG_ID, "domain-dns", "impSpf"))
                .thenReturn(Optional.of(row("u1", "domain-dns", "impSpf", true, "REQUIRED")));

        service.setPreference(USER_EMAIL, "domain-dns", "impSpf", true, "  ");

        verify(repository).delete(any(UserSecurityPreference.class));
        verify(repository, never()).save(any());
    }

    @Test
    void setPreference_dnsValue_savesPreferenceValue() {
        when(repository.findByOrganizationIdAndSectionAndPreferenceKey(ORG_ID, "domain-dns", "impMx"))
                .thenReturn(Optional.empty());

        service.setPreference(USER_EMAIL, "domain-dns", "impMx", true, " RECOMMENDED ");

        verify(repository).save(any(UserSecurityPreference.class));
    }

    @Test
    void getPreferencesResponse_emptyRepo_allTogglesTrue() {
        when(repository.findByOrganizationId(ORG_ID)).thenReturn(List.of());

        var response = service.getPreferencesResponse(USER_EMAIL);

        assertTrue(response.preferences().get("users-groups:2fa"));
        assertTrue(response.preferences().get("password-settings:2sv"));
        assertEquals("REQUIRED", response.dnsImportance().get("SPF"));
        assertTrue(response.dnsImportanceOverrideTypes().isEmpty());
    }

    @Test
    void getPreferencesResponse_knownRowsOverrideDefaults_unknownRowsIgnored() {
        when(repository.findByOrganizationId(ORG_ID)).thenReturn(List.of(
                row("u1", "users-groups", "2fa", false, null),
                row("u1", "custom-section", "customKey", false, null)
        ));

        var response = service.getPreferencesResponse(USER_EMAIL);

        assertFalse(response.preferences().get("users-groups:2fa"));
        assertTrue(response.preferences().get("password-settings:2sv"));
        assertFalse(response.preferences().containsKey("custom-section:customKey"));
    }

    @Test
    void getPreferencesResponse_dnsImportanceOverrideTypes_listsTypesWithStoredValue() {
        when(repository.findByOrganizationId(ORG_ID)).thenReturn(List.of());
        when(repository.findByOrganizationIdAndSection(ORG_ID, "domain-dns")).thenReturn(List.of(
                row("u1", "domain-dns", "impMx", true, "RECOMMENDED"),
                row("u1", "domain-dns", "impSpf", true, "  "),
                row("u1", "domain-dns", "impDkim", true, "OPTIONAL")
        ));

        var response = service.getPreferencesResponse(USER_EMAIL);

        assertEquals(Set.of("MX", "DKIM"), response.dnsImportanceOverrideTypes());
        assertEquals("RECOMMENDED", response.dnsImportance().get("MX"));
        assertEquals("OPTIONAL", response.dnsImportance().get("DKIM"));
    }

    @Test
    void getDisabledPreferenceKeys_includesDisabledDnsImportanceRows() {
        when(repository.findByOrganizationId(ORG_ID)).thenReturn(List.of(
                row("u1", "domain-dns", "impSpf", false, "REQUIRED")
        ));

        assertEquals(Set.of("domain-dns:impSpf"), service.getDisabledPreferenceKeys(USER_EMAIL));
    }

    @Test
    void isNotificationHiddenByPreference_unmappedNotification_neverHidden() {
        when(repository.findByOrganizationId(ORG_ID)).thenReturn(List.of(
                row("u1", "password-settings", "2sv", false, null)
        ));

        assertFalse(service.isNotificationHiddenByPreference(
                USER_EMAIL, "password-settings", "nonexistent-type"));
    }

    @Test
    void setPreference_booleanOverload_returnsPersistedEntity() {
        final UserSecurityPreference[] holder = new UserSecurityPreference[1];
        when(repository.findByOrganizationIdAndSectionAndPreferenceKey(ORG_ID, "users-groups", "2fa"))
                .thenReturn(Optional.empty())
                .thenAnswer(inv -> Optional.of(holder[0]));
        when(repository.save(any(UserSecurityPreference.class))).thenAnswer(inv -> {
            holder[0] = inv.getArgument(0);
            return holder[0];
        });

        UserSecurityPreference out = service.setPreference(USER_EMAIL, "users-groups", "2fa", true);

        assertTrue(out.isEnabled());
        assertEquals(ORG_ID, out.getOrganizationId());
        assertEquals("users-groups", out.getSection());
        assertEquals("2fa", out.getPreferenceKey());
        verify(repository).save(any(UserSecurityPreference.class));
    }

    @Test
    void setPreference_booleanBranch_clearsPreferenceValueForNonDns() {
        UserSecurityPreference existing = row("u1", "users-groups", "activity", true, "legacy");
        when(repository.findByOrganizationIdAndSectionAndPreferenceKey(ORG_ID, "users-groups", "activity"))
                .thenReturn(Optional.of(existing));
        when(repository.save(any(UserSecurityPreference.class))).thenAnswer(inv -> inv.getArgument(0));

        service.setPreference(USER_EMAIL, "users-groups", "activity", false);

        assertFalse(existing.isEnabled());
        assertEquals(null, existing.getPreferenceValue());
        verify(repository).save(existing);
    }

    @Test
    void setSectionPreferences_appliesEachKey() {
        when(repository.findByOrganizationIdAndSectionAndPreferenceKey(eq(ORG_ID), any(), any()))
                .thenReturn(Optional.empty());
        when(repository.save(any(UserSecurityPreference.class))).thenAnswer(inv -> inv.getArgument(0));

        service.setSectionPreferences(USER_EMAIL, "app-access",
                Map.of("highRisk", false, "unknownKeyInSection", true));

        verify(repository, times(2)).save(any(UserSecurityPreference.class));
    }

    @Test
    void setPreference_dnsInvalidEnum_rejectsBeforePersist() {
        when(messageSource.getMessage(
                        eq("api.preferences.validation.dns_importance_invalid"), isNull(), any()))
                .thenReturn("Invalid DNS importance");

        SecurityPreferenceValidationException ex =
                assertThrows(
                        SecurityPreferenceValidationException.class,
                        () -> service.setPreference(USER_EMAIL, "domain-dns", "impMx", true, "NOT_AN_ENUM"));
        assertEquals("Invalid DNS importance", ex.getMessage());
        verify(repository, never()).save(any());
    }

    @Test
    void setPreference_blankSection_throws() {
        when(messageSource.getMessage(eq("api.preferences.validation.section_required"), isNull(), any()))
                .thenReturn("Section required");

        assertThrows(
                SecurityPreferenceValidationException.class,
                () -> service.setPreference(USER_EMAIL, " ", "2fa", true, null));
        verifyNoInteractions(repository);
    }

    @Test
    void setPreference_blankPreferenceKey_throws() {
        when(messageSource.getMessage(eq("api.preferences.validation.preference_key_required"), isNull(), any()))
                .thenReturn("Key required");

        assertThrows(
                SecurityPreferenceValidationException.class,
                () -> service.setPreference(USER_EMAIL, "users-groups", "  ", true, null));
        verifyNoInteractions(repository);
    }

    private static UserSecurityPreference row(
            String userId, String section, String key, boolean enabled, String value) {
        UserSecurityPreference p = new UserSecurityPreference();
        p.setUserId(userId);
        p.setOrganizationId(ORG_ID);
        p.setSection(section);
        p.setPreferenceKey(key);
        p.setEnabled(enabled);
        p.setPreferenceValue(value);
        return p;
    }
}
