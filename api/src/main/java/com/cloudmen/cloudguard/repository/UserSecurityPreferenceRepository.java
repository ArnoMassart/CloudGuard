package com.cloudmen.cloudguard.repository;

import com.cloudmen.cloudguard.domain.model.preference.UserSecurityPreference;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

/**
 * Persistence for {@link UserSecurityPreference} (organization + section + preference key uniqueness).
 */
public interface UserSecurityPreferenceRepository extends JpaRepository<UserSecurityPreference, Long> {

    List<UserSecurityPreference> findByOrganizationId(Long organizationId);

    List<UserSecurityPreference> findByOrganizationIdAndSection(Long organizationId, String section);

    Optional<UserSecurityPreference> findByOrganizationIdAndSectionAndPreferenceKey(Long organizationId, String section, String preferenceKey);

    /** Cascade cleanup when an organization is removed. */
    void deleteByOrganizationId(Long organizationId);
}
