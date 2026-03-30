package com.cloudmen.cloudguard.service.preference;

import com.cloudmen.cloudguard.domain.model.preference.UserSecurityPreference;
import com.cloudmen.cloudguard.repository.UserSecurityPreferenceRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class UserSecurityPreferenceServiceTest {

    @Mock
    UserSecurityPreferenceRepository repository;

    UserSecurityPreferenceService service;

    @BeforeEach
    void setUp() {
        service = new UserSecurityPreferenceService(repository);
    }

    @Test
    void getDisabledPreferenceKeys_onlyDisabledRows() {
        UserSecurityPreference on = row("u1", "users-groups", "2fa", true, null);
        UserSecurityPreference off = row("u1", "users-groups", "activity", false, null);
        when(repository.findByUserId("u1")).thenReturn(List.of(on, off));

        Set<String> disabled = service.getDisabledPreferenceKeys("u1");
        assertEquals(Set.of("users-groups:activity"), disabled);
    }

    @Test
    void effectiveDnsImportanceDisplay_mergesOverridesWithDefaults() {
        UserSecurityPreference spfOverride = row("u1", "domain-dns", "impSpf", true, "OPTIONAL");
        when(repository.findByUserIdAndSection("u1", "domain-dns")).thenReturn(List.of(spfOverride));

        Map<String, String> display = service.effectiveDnsImportanceDisplay("u1");
        assertEquals("OPTIONAL", display.get("SPF"));
        assertEquals("REQUIRED", display.get("MX"));
        assertEquals("OPTIONAL", display.get("TXT"));
    }

    @Test
    void getPreferencesForSection_skipsDnsImpKeys() {
        when(repository.findByUserIdAndSection("u1", "domain-dns")).thenReturn(List.of(
                row("u1", "domain-dns", "impSpf", true, "REQUIRED"),
                row("u1", "domain-dns", "someToggle", false, null)
        ));

        Map<String, Boolean> section = service.getPreferencesForSection("u1", "domain-dns");
        assertEquals(Map.of("someToggle", false), section);
    }

    @Test
    void isNotificationHiddenByPreference_tracksDisabledMapping() {
        when(repository.findByUserId("u1")).thenReturn(List.of(
                row("u1", "password-settings", "2sv", false, null)
        ));

        assertTrue(service.isNotificationHiddenByPreference(
                "u1", "password-settings", "password-2sv-not-enforced"));
        assertFalse(service.isNotificationHiddenByPreference(
                "u1", "password-settings", "password-weak-length"));
    }

    @Test
    void setPreference_dnsBlankValue_deletesRow() {
        when(repository.findByUserIdAndSectionAndPreferenceKey("u1", "domain-dns", "impSpf"))
                .thenReturn(Optional.of(row("u1", "domain-dns", "impSpf", true, "REQUIRED")));

        service.setPreference("u1", "domain-dns", "impSpf", true, "  ");

        verify(repository).delete(any(UserSecurityPreference.class));
        verify(repository, never()).save(any());
    }

    @Test
    void setPreference_dnsValue_savesPreferenceValue() {
        when(repository.findByUserIdAndSectionAndPreferenceKey("u1", "domain-dns", "impMx"))
                .thenReturn(Optional.empty());

        service.setPreference("u1", "domain-dns", "impMx", true, " RECOMMENDED ");

        verify(repository).save(any(UserSecurityPreference.class));
    }

    @Test
    void getPreferencesResponse_emptyRepo_allTogglesTrue() {
        when(repository.findByUserId("u1")).thenReturn(List.of());

        var response = service.getPreferencesResponse("u1");

        assertTrue(response.preferences().get("users-groups:2fa"));
        assertTrue(response.preferences().get("password-settings:2sv"));
        assertEquals("REQUIRED", response.dnsImportance().get("SPF"));
        assertTrue(response.dnsImportanceOverrideTypes().isEmpty());
    }

    @Test
    void getPreferencesResponse_knownRowsOverrideDefaults_unknownRowsIgnored() {
        when(repository.findByUserId("u1")).thenReturn(List.of(
                row("u1", "users-groups", "2fa", false, null),
                row("u1", "custom-section", "customKey", false, null)
        ));

        var response = service.getPreferencesResponse("u1");

        assertFalse(response.preferences().get("users-groups:2fa"));
        assertTrue(response.preferences().get("password-settings:2sv"));
        assertFalse(response.preferences().containsKey("custom-section:customKey"));
    }

    @Test
    void getPreferencesResponse_dnsImportanceOverrideTypes_listsTypesWithStoredValue() {
        when(repository.findByUserId("u1")).thenReturn(List.of());
        when(repository.findByUserIdAndSection("u1", "domain-dns")).thenReturn(List.of(
                row("u1", "domain-dns", "impMx", true, "RECOMMENDED"),
                row("u1", "domain-dns", "impSpf", true, "  "),
                row("u1", "domain-dns", "impDkim", true, "OPTIONAL")
        ));

        var response = service.getPreferencesResponse("u1");

        assertEquals(Set.of("MX", "DKIM"), response.dnsImportanceOverrideTypes());
        assertEquals("RECOMMENDED", response.dnsImportance().get("MX"));
        assertEquals("OPTIONAL", response.dnsImportance().get("DKIM"));
    }

    @Test
    void getDisabledPreferenceKeys_includesDisabledDnsImportanceRows() {
        when(repository.findByUserId("u1")).thenReturn(List.of(
                row("u1", "domain-dns", "impSpf", false, "REQUIRED")
        ));

        assertEquals(Set.of("domain-dns:impSpf"), service.getDisabledPreferenceKeys("u1"));
    }

    @Test
    void isNotificationHiddenByPreference_unmappedNotification_neverHidden() {
        when(repository.findByUserId("u1")).thenReturn(List.of(
                row("u1", "password-settings", "2sv", false, null)
        ));

        assertFalse(service.isNotificationHiddenByPreference(
                "u1", "password-settings", "nonexistent-type"));
    }

    @Test
    void setPreference_booleanOverload_returnsPersistedEntity() {
        final UserSecurityPreference[] holder = new UserSecurityPreference[1];
        when(repository.findByUserIdAndSectionAndPreferenceKey("u1", "users-groups", "2fa"))
                .thenReturn(Optional.empty())
                .thenAnswer(inv -> Optional.of(holder[0]));
        when(repository.save(any(UserSecurityPreference.class))).thenAnswer(inv -> {
            holder[0] = inv.getArgument(0);
            return holder[0];
        });

        UserSecurityPreference out = service.setPreference("u1", "users-groups", "2fa", true);

        assertTrue(out.isEnabled());
        assertEquals("u1", out.getUserId());
        assertEquals("users-groups", out.getSection());
        assertEquals("2fa", out.getPreferenceKey());
        verify(repository).save(any(UserSecurityPreference.class));
    }

    @Test
    void setPreference_booleanBranch_clearsPreferenceValueForNonDns() {
        UserSecurityPreference existing = row("u1", "users-groups", "activity", true, "legacy");
        when(repository.findByUserIdAndSectionAndPreferenceKey("u1", "users-groups", "activity"))
                .thenReturn(Optional.of(existing));
        when(repository.save(any(UserSecurityPreference.class))).thenAnswer(inv -> inv.getArgument(0));

        service.setPreference("u1", "users-groups", "activity", false);

        assertFalse(existing.isEnabled());
        assertEquals(null, existing.getPreferenceValue());
        verify(repository).save(existing);
    }

    @Test
    void setSectionPreferences_appliesEachKey() {
        when(repository.findByUserIdAndSectionAndPreferenceKey(any(), any(), any()))
                .thenReturn(Optional.empty());
        when(repository.save(any(UserSecurityPreference.class))).thenAnswer(inv -> inv.getArgument(0));

        service.setSectionPreferences("u1", "app-access",
                Map.of("highRisk", false, "unknownKeyInSection", true));

        verify(repository, times(2)).save(any(UserSecurityPreference.class));
    }

    @Test
    void setPreference_dnsInvalidEnum_rejectsBeforePersist() {
        assertThrows(IllegalArgumentException.class,
                () -> service.setPreference("u1", "domain-dns", "impMx", true, "NOT_AN_ENUM"));
        verify(repository, never()).save(any());
    }

    private static UserSecurityPreference row(
            String userId, String section, String key, boolean enabled, String value) {
        UserSecurityPreference p = new UserSecurityPreference();
        p.setUserId(userId);
        p.setSection(section);
        p.setPreferenceKey(key);
        p.setEnabled(enabled);
        p.setPreferenceValue(value);
        return p;
    }
}
