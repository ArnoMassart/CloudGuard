package com.cloudmen.cloudguard.service.preference;

import com.cloudmen.cloudguard.domain.model.DnsRecordImportance;
import com.cloudmen.cloudguard.domain.model.preference.UserSecurityPreference;
import com.cloudmen.cloudguard.dto.preferences.PreferencesResponse;
import com.cloudmen.cloudguard.repository.UserSecurityPreferenceRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

@Service
public class UserSecurityPreferenceService {

    private final UserSecurityPreferenceRepository repository;

    public UserSecurityPreferenceService(UserSecurityPreferenceRepository repository) {
        this.repository = repository;
    }

    /**
     * Returns the set of "section:preferenceKey" that are disabled for this user.
     * Default is enabled when no row exists.
     */
    public Set<String> getDisabledPreferenceKeys(String userId) {
        return repository.findByUserId(userId).stream()
                .filter(p -> !p.isEnabled())
                .map(p -> p.getSection() + ":" + p.getPreferenceKey())
                .collect(Collectors.toSet());
    }

    /**
     * Boolean toggles plus effective DNS importance per record type (for UI and API).
     */
    public PreferencesResponse getPreferencesResponse(String userId) {
        Map<String, Boolean> bools = new LinkedHashMap<>();
        for (String key : PreferenceToNotificationMapping.getAllPreferenceKeys()) {
            bools.put(key, true);
        }
        for (UserSecurityPreference p : repository.findByUserId(userId)) {
            if ("domain-dns".equals(p.getSection()) && DnsImportancePreferenceSupport.isDnsImportancePreferenceKey(p.getPreferenceKey())) {
                continue;
            }
            String full = p.getSection() + ":" + p.getPreferenceKey();
            if (PreferenceToNotificationMapping.getAllPreferenceKeys().contains(full)) {
                bools.put(full, p.isEnabled());
            }
        }
        return new PreferencesResponse(bools, effectiveDnsImportanceDisplay(userId), dnsTypesWithStoredImportance(userId));
    }

    private Set<String> dnsTypesWithStoredImportance(String userId) {
        Set<String> overridden = new HashSet<>();
        for (UserSecurityPreference p : repository.findByUserIdAndSection(userId, "domain-dns")) {
            if (!DnsImportancePreferenceSupport.isDnsImportancePreferenceKey(p.getPreferenceKey())) {
                continue;
            }
            if (p.getPreferenceValue() == null || p.getPreferenceValue().isBlank()) {
                continue;
            }
            String t = DnsImportancePreferenceSupport.dnsTypeForPreferenceKey(p.getPreferenceKey());
            if (t != null) {
                overridden.add(t);
            }
        }
        return Set.copyOf(overridden);
    }

    /**
     * Effective importance per DNS type (merge user override with system default).
     */
    public Map<String, String> effectiveDnsImportanceDisplay(String userId) {
        Map<String, DnsRecordImportance> overrides = getDnsImportanceOverrides(userId);
        Map<String, String> out = new LinkedHashMap<>();
        for (String type : DnsImportancePreferenceSupport.DNS_TYPES) {
            DnsRecordImportance eff = overrides.getOrDefault(type, DnsImportancePreferenceSupport.systemDefaultImportance(type));
            out.put(type, eff.name());
        }
        return out;
    }

    /**
     * Map DNS record type → user override only (empty if no row). Used when building DNS responses.
     */
    public Map<String, DnsRecordImportance> getDnsImportanceOverrides(String userId) {
        return DnsImportancePreferenceSupport.parseOverridesFromDbRows(
                repository.findByUserIdAndSection(userId, "domain-dns"));
    }

    /**
     * @deprecated Use {@link #getPreferencesResponse(String)} for the full payload.
     */
    @Deprecated
    public Map<String, Boolean> getAllPreferences(String userId) {
        return getPreferencesResponse(userId).preferences();
    }

    /**
     * Returns preferences for a specific section.
     */
    public Map<String, Boolean> getPreferencesForSection(String userId, String section) {
        Map<String, Boolean> result = new LinkedHashMap<>();
        for (UserSecurityPreference p : repository.findByUserIdAndSection(userId, section)) {
            if (DnsImportancePreferenceSupport.isDnsImportancePreferenceKey(p.getPreferenceKey())) {
                continue;
            }
            result.put(p.getPreferenceKey(), p.isEnabled());
        }
        return result;
    }

    /**
     * Set a boolean preference or a DNS importance value (domain-dns / imp* keys).
     */
    @Transactional
    public void setPreference(String userId, String section, String preferenceKey, Boolean enabled, String value) {
        if ("domain-dns".equals(section) && DnsImportancePreferenceSupport.isDnsImportancePreferenceKey(preferenceKey)) {
            if (value == null || value.isBlank()) {
                repository.findByUserIdAndSectionAndPreferenceKey(userId, section, preferenceKey)
                        .ifPresent(repository::delete);
                return;
            }
            DnsRecordImportance.valueOf(value.trim());
            UserSecurityPreference p = repository.findByUserIdAndSectionAndPreferenceKey(userId, section, preferenceKey)
                    .orElseGet(() -> {
                        UserSecurityPreference newP = new UserSecurityPreference();
                        newP.setUserId(userId);
                        newP.setSection(section);
                        newP.setPreferenceKey(preferenceKey);
                        return newP;
                    });
            p.setPreferenceValue(value.trim());
            p.setEnabled(true);
            p.setUpdatedAt(LocalDateTime.now());
            repository.save(p);
            return;
        }

        boolean en = enabled != null ? enabled : true;
        UserSecurityPreference p = repository.findByUserIdAndSectionAndPreferenceKey(userId, section, preferenceKey)
                .orElseGet(() -> {
                    UserSecurityPreference newP = new UserSecurityPreference();
                    newP.setUserId(userId);
                    newP.setSection(section);
                    newP.setPreferenceKey(preferenceKey);
                    return newP;
                });
        p.setEnabled(en);
        p.setPreferenceValue(null);
        p.setUpdatedAt(LocalDateTime.now());
        repository.save(p);
    }

    /**
     * Set a single boolean preference (no DNS importance).
     */
    @Transactional
    public UserSecurityPreference setPreference(String userId, String section, String preferenceKey, boolean enabled) {
        setPreference(userId, section, preferenceKey, enabled, null);
        return repository.findByUserIdAndSectionAndPreferenceKey(userId, section, preferenceKey).orElseThrow();
    }

    /**
     * Set multiple preferences for a section at once (boolean only).
     */
    @Transactional
    public void setSectionPreferences(String userId, String section, Map<String, Boolean> preferences) {
        for (Map.Entry<String, Boolean> e : preferences.entrySet()) {
            setPreference(userId, section, e.getKey(), e.getValue(), null);
        }
    }

    /**
     * Check if a notification (source, notificationType) should be hidden based on user preferences.
     */
    public boolean isNotificationHiddenByPreference(String userId, String source, String notificationType) {
        Set<String> disabled = getDisabledPreferenceKeys(userId);
        return PreferenceToNotificationMapping.isDisabledByPreference(source, notificationType, disabled);
    }
}
