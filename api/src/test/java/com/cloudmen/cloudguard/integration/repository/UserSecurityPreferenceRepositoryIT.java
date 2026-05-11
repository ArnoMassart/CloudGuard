package com.cloudmen.cloudguard.integration.repository;

import com.cloudmen.cloudguard.domain.model.preference.UserSecurityPreference;
import com.cloudmen.cloudguard.repository.UserSecurityPreferenceRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Smoke tests for {@link UserSecurityPreferenceRepository} mapping and composite lookup.
 *
 * <p>Queries are derived — low JPQL risk — but the unique constraint
 * {@code (organization_id, section, preference_key)} is easy to break during refactors.
 */
class UserSecurityPreferenceRepositoryIT extends AbstractRepositoryIT {

    @Autowired
    private UserSecurityPreferenceRepository userSecurityPreferenceRepository;

    @BeforeEach
    void cleanSlate() {
        userSecurityPreferenceRepository.deleteAll();
    }

    @Test
    @DisplayName("findByOrganizationIdAndSectionAndPreferenceKey returns the matching preference")
    void findByOrganizationIdAndSectionAndPreferenceKey_returnsRow() {
        userSecurityPreferenceRepository.save(pref(10L, "dns", "spf_importance", "REQUIRED"));
        userSecurityPreferenceRepository.save(pref(10L, "dns", "dkim_importance", "OPTIONAL"));
        userSecurityPreferenceRepository.save(pref(99L, "dns", "spf_importance", "RECOMMENDED"));

        Optional<UserSecurityPreference> found =
                userSecurityPreferenceRepository.findByOrganizationIdAndSectionAndPreferenceKey(
                        10L, "dns", "spf_importance");

        assertThat(found)
                .isPresent()
                .get()
                .satisfies(
                        p -> {
                            assertThat(p.getPreferenceValue()).isEqualTo("REQUIRED");
                            assertThat(p.isEnabled()).isTrue();
                        });
    }

    @Test
    @DisplayName("findByOrganizationIdAndSection lists only preferences for that org and section")
    void findByOrganizationIdAndSection_filtersByOrgAndSection() {
        userSecurityPreferenceRepository.save(pref(1L, "section-a", "k1", null));
        userSecurityPreferenceRepository.save(pref(1L, "section-a", "k2", null));
        userSecurityPreferenceRepository.save(pref(1L, "section-b", "k3", null));

        List<UserSecurityPreference> sectionA =
                userSecurityPreferenceRepository.findByOrganizationIdAndSection(1L, "section-a");

        assertThat(sectionA).hasSize(2);
        assertThat(sectionA).extracting(UserSecurityPreference::getPreferenceKey).containsExactlyInAnyOrder("k1", "k2");
    }

    @Test
    @DisplayName("deleteByOrganizationId removes every preference row for that tenant")
    void deleteByOrganizationId_removesAllForOrganization() {
        userSecurityPreferenceRepository.save(pref(5L, "s", "a", null));
        userSecurityPreferenceRepository.save(pref(5L, "s", "b", null));
        userSecurityPreferenceRepository.save(pref(6L, "s", "c", null));

        userSecurityPreferenceRepository.deleteByOrganizationId(5L);

        assertThat(userSecurityPreferenceRepository.findByOrganizationId(5L)).isEmpty();
        assertThat(userSecurityPreferenceRepository.findByOrganizationId(6L)).hasSize(1);
    }

    private static UserSecurityPreference pref(
            Long orgId, String section, String key, String value) {
        UserSecurityPreference p = new UserSecurityPreference();
        p.setOrganizationId(orgId);
        p.setSection(section);
        p.setPreferenceKey(key);
        p.setPreferenceValue(value);
        p.setEnabled(true);
        return p;
    }
}
