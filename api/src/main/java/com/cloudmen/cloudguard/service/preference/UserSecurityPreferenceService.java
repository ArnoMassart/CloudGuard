package com.cloudmen.cloudguard.service.preference;

import com.cloudmen.cloudguard.domain.model.DnsRecordImportance;
import com.cloudmen.cloudguard.domain.model.User;
import com.cloudmen.cloudguard.domain.model.preference.UserSecurityPreference;
import com.cloudmen.cloudguard.dto.preferences.PreferencesResponse;
import com.cloudmen.cloudguard.exception.OrganizationRequiredException;
import com.cloudmen.cloudguard.exception.SecurityPreferenceValidationException;
import com.cloudmen.cloudguard.exception.UnauthorizedException;
import com.cloudmen.cloudguard.repository.UserRepository;
import com.cloudmen.cloudguard.repository.UserSecurityPreferenceRepository;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.MessageSource;
import org.springframework.context.i18n.LocaleContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Collections;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

@Service
public class UserSecurityPreferenceService {

    private final UserSecurityPreferenceRepository repository;
    private final UserRepository userRepository;
    private final MessageSource messageSource;

    public UserSecurityPreferenceService(
            UserSecurityPreferenceRepository repository,
            UserRepository userRepository,
            @Qualifier("messageSource") MessageSource messageSource) {
        this.repository = repository;
        this.userRepository = userRepository;
        this.messageSource = messageSource;
    }

    private User requireUser(String userEmail) {
        return userRepository.findByEmail(userEmail)
                .orElseThrow(() -> new UnauthorizedException(
                        messageSource.getMessage(
                                "api.auth.session_invalid", null, LocaleContextHolder.getLocale())));
    }

    private Long requireOrganizationId(String userEmail) {
        User user = requireUser(userEmail);
        if (user.getOrganizationId() == null) {
            throw new OrganizationRequiredException(
                    messageSource.getMessage(
                            "api.preferences.organization_required", null, LocaleContextHolder.getLocale()));
        }
        return user.getOrganizationId();
    }

    private Optional<Long> organizationIdForRead(String userEmail) {
        Long orgId = requireUser(userEmail).getOrganizationId();
        return orgId != null ? Optional.of(orgId) : Optional.empty();
    }

    /**
     * Defaults when the user is not yet linked to a Workspace organization: all toggles on, system DNS
     * importance only (no org-scoped rows).
     */
    private PreferencesResponse preferencesResponseWithoutOrganization() {
        Map<String, Boolean> bools = new LinkedHashMap<>();
        for (String key : PreferenceToNotificationMapping.getAllPreferenceKeys()) {
            bools.put(key, true);
        }
        Map<String, String> dns = new LinkedHashMap<>();
        for (String type : DnsImportancePreferenceSupport.DNS_TYPES) {
            dns.put(type, DnsImportancePreferenceSupport.systemDefaultImportance(type).name());
        }
        return new PreferencesResponse(bools, dns, Set.of());
    }

    /**
     * Returns the set of "section:preferenceKey" that are disabled for the signed-in user's organization.
     * Default is enabled when no row exists. When the user has no organization yet, returns an empty set
     * so dashboards use defaults.
     */
    public Set<String> getDisabledPreferenceKeys(String userEmail) {
        return organizationIdForRead(userEmail)
                .map(
                        orgId ->
                                repository.findByOrganizationId(orgId).stream()
                                        .filter(p -> !p.isEnabled())
                                        .map(p -> p.getSection() + ":" + p.getPreferenceKey())
                                        .collect(Collectors.toSet()))
                .orElseGet(Set::of);
    }

    /**
     * Boolean toggles plus effective DNS importance per record type (for UI and API).
     */
    public PreferencesResponse getPreferencesResponse(String userEmail) {
        Optional<Long> orgIdOpt = organizationIdForRead(userEmail);
        if (orgIdOpt.isEmpty()) {
            return preferencesResponseWithoutOrganization();
        }
        Long orgId = orgIdOpt.get();
        Map<String, Boolean> bools = new LinkedHashMap<>();
        for (String key : PreferenceToNotificationMapping.getAllPreferenceKeys()) {
            bools.put(key, true);
        }
        for (UserSecurityPreference p : repository.findByOrganizationId(orgId)) {
            if ("domain-dns".equals(p.getSection())
                    && DnsImportancePreferenceSupport.isDnsImportancePreferenceKey(p.getPreferenceKey())) {
                continue;
            }
            String full = p.getSection() + ":" + p.getPreferenceKey();
            if (PreferenceToNotificationMapping.getAllPreferenceKeys().contains(full)) {
                bools.put(full, p.isEnabled());
            }
        }
        return new PreferencesResponse(bools, effectiveDnsImportanceDisplay(orgId), dnsTypesWithStoredImportance(orgId));
    }

    private Set<String> dnsTypesWithStoredImportance(Long organizationId) {
        Set<String> overridden = new HashSet<>();
        for (UserSecurityPreference p : repository.findByOrganizationIdAndSection(organizationId, "domain-dns")) {
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
     * Effective importance per DNS type (merge organization override with system default).
     */
    public Map<String, String> effectiveDnsImportanceDisplay(String userEmail) {
        return organizationIdForRead(userEmail)
                .map(this::effectiveDnsImportanceDisplay)
                .orElseGet(
                        () -> {
                            Map<String, String> dns = new LinkedHashMap<>();
                            for (String type : DnsImportancePreferenceSupport.DNS_TYPES) {
                                dns.put(
                                        type,
                                        DnsImportancePreferenceSupport.systemDefaultImportance(type).name());
                            }
                            return dns;
                        });
    }

    private Map<String, String> effectiveDnsImportanceDisplay(Long organizationId) {
        Map<String, DnsRecordImportance> overrides = getDnsImportanceOverrides(organizationId);
        Map<String, String> out = new LinkedHashMap<>();
        for (String type : DnsImportancePreferenceSupport.DNS_TYPES) {
            DnsRecordImportance eff = overrides.getOrDefault(type, DnsImportancePreferenceSupport.systemDefaultImportance(type));
            out.put(type, eff.name());
        }
        return out;
    }

    /**
     * Map DNS record type → organization override only (empty if no row). Used when building DNS responses.
     */
    public Map<String, DnsRecordImportance> getDnsImportanceOverrides(String userEmail) {
        return organizationIdForRead(userEmail)
                .map(this::getDnsImportanceOverrides)
                .orElseGet(Collections::emptyMap);
    }

    private Map<String, DnsRecordImportance> getDnsImportanceOverrides(Long organizationId) {
        return DnsImportancePreferenceSupport.parseOverridesFromDbRows(
                repository.findByOrganizationIdAndSection(organizationId, "domain-dns"));
    }

    /**
     * @deprecated Use {@link #getPreferencesResponse(String)} for the full payload.
     */
    @Deprecated
    public Map<String, Boolean> getAllPreferences(String userEmail) {
        return getPreferencesResponse(userEmail).preferences();
    }

    /**
     * Returns preferences for a specific section.
     */
    public Map<String, Boolean> getPreferencesForSection(String userEmail, String section) {
        Optional<Long> orgIdOpt = organizationIdForRead(userEmail);
        if (orgIdOpt.isEmpty()) {
            return new LinkedHashMap<>();
        }
        Long orgId = orgIdOpt.get();
        Map<String, Boolean> result = new LinkedHashMap<>();
        for (UserSecurityPreference p : repository.findByOrganizationIdAndSection(orgId, section)) {
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
    public void setPreference(String userEmail, String section, String preferenceKey, Boolean enabled, String value) {
        setPreferenceForOrganization(requireOrganizationId(userEmail), section, preferenceKey, enabled, value);
    }

    private void setPreferenceForOrganization(
            Long orgId, String section, String preferenceKey, Boolean enabled, String value) {
        validateSection(section);
        validatePreferenceKey(preferenceKey);

        if ("domain-dns".equals(section) && DnsImportancePreferenceSupport.isDnsImportancePreferenceKey(preferenceKey)) {
            if (value == null || value.isBlank()) {
                repository.findByOrganizationIdAndSectionAndPreferenceKey(orgId, section, preferenceKey)
                        .ifPresent(repository::delete);
                return;
            }
            try {
                DnsRecordImportance.valueOf(value.trim());
            } catch (IllegalArgumentException e) {
                throw new SecurityPreferenceValidationException(
                        messageSource.getMessage(
                                "api.preferences.validation.dns_importance_invalid",
                                null,
                                LocaleContextHolder.getLocale()));
            }
            UserSecurityPreference p = repository.findByOrganizationIdAndSectionAndPreferenceKey(orgId, section, preferenceKey)
                    .orElseGet(() -> {
                        UserSecurityPreference newP = new UserSecurityPreference();
                        newP.setOrganizationId(orgId);
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
        UserSecurityPreference p = repository.findByOrganizationIdAndSectionAndPreferenceKey(orgId, section, preferenceKey)
                .orElseGet(() -> {
                    UserSecurityPreference newP = new UserSecurityPreference();
                    newP.setOrganizationId(orgId);
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
    public UserSecurityPreference setPreference(String userEmail, String section, String preferenceKey, boolean enabled) {
        Long orgId = requireOrganizationId(userEmail);
        setPreferenceForOrganization(orgId, section, preferenceKey, enabled, null);
        return repository.findByOrganizationIdAndSectionAndPreferenceKey(orgId, section, preferenceKey).orElseThrow();
    }

    /**
     * Set multiple preferences for a section at once (boolean only).
     */
    @Transactional
    public void setSectionPreferences(String userEmail, String section, Map<String, Boolean> preferences) {
        Long orgId = requireOrganizationId(userEmail);
        for (Map.Entry<String, Boolean> e : preferences.entrySet()) {
            setPreferenceForOrganization(orgId, section, e.getKey(), e.getValue(), null);
        }
    }

    /**
     * Check if a notification (source, notificationType) should be hidden based on user preferences.
     */
    public boolean isNotificationHiddenByPreference(String userEmail, String source, String notificationType) {
        Set<String> disabled = getDisabledPreferenceKeys(userEmail);
        return PreferenceToNotificationMapping.isDisabledByPreference(source, notificationType, disabled);
    }

    private void validateSection(String section) {
        if (section == null || section.isBlank()) {
            throw new SecurityPreferenceValidationException(
                    messageSource.getMessage(
                            "api.preferences.validation.section_required", null, LocaleContextHolder.getLocale()));
        }
    }

    private void validatePreferenceKey(String preferenceKey) {
        if (preferenceKey == null || preferenceKey.isBlank()) {
            throw new SecurityPreferenceValidationException(
                    messageSource.getMessage(
                            "api.preferences.validation.preference_key_required",
                            null,
                            LocaleContextHolder.getLocale()));
        }
    }
}
