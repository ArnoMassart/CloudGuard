package com.cloudmen.cloudguard.service.preference;

import com.cloudmen.cloudguard.domain.model.preference.UserSecurityPreference;
import com.cloudmen.cloudguard.repository.UserSecurityPreferenceRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.*;
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
     * Returns all preferences for the user. Missing preferences default to enabled.
     */
    public Map<String, Boolean> getAllPreferences(String userId) {
        Map<String, Boolean> result = new LinkedHashMap<>();
        Set<String> allKeys = PreferenceToNotificationMapping.getAllPreferenceKeys();
        for (String key : allKeys) {
            result.put(key, true); // default
        }
        for (UserSecurityPreference p : repository.findByUserId(userId)) {
            String key = p.getSection() + ":" + p.getPreferenceKey();
            result.put(key, p.isEnabled());
        }
        return result;
    }

    /**
     * Returns preferences for a specific section.
     */
    public Map<String, Boolean> getPreferencesForSection(String userId, String section) {
        Map<String, Boolean> result = new LinkedHashMap<>();
        for (UserSecurityPreference p : repository.findByUserIdAndSection(userId, section)) {
            result.put(p.getPreferenceKey(), p.isEnabled());
        }
        return result;
    }

    /**
     * Set a single preference. Creates or updates the row.
     */
    @Transactional
    public UserSecurityPreference setPreference(String userId, String section, String preferenceKey, boolean enabled) {
        UserSecurityPreference p = repository.findByUserIdAndSectionAndPreferenceKey(userId, section, preferenceKey)
                .orElseGet(() -> {
                    UserSecurityPreference newP = new UserSecurityPreference();
                    newP.setUserId(userId);
                    newP.setSection(section);
                    newP.setPreferenceKey(preferenceKey);
                    return newP;
                });
        p.setEnabled(enabled);
        p.setUpdatedAt(LocalDateTime.now());
        return repository.save(p);
    }

    /**
     * Set multiple preferences for a section at once.
     */
    @Transactional
    public void setSectionPreferences(String userId, String section, Map<String, Boolean> preferences) {
        for (Map.Entry<String, Boolean> e : preferences.entrySet()) {
            setPreference(userId, section, e.getKey(), e.getValue());
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
