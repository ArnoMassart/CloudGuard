package com.cloudmen.cloudguard.repository;

import com.cloudmen.cloudguard.domain.model.preference.UserSecurityPreference;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface UserSecurityPreferenceRepository extends JpaRepository<UserSecurityPreference, Long> {

    List<UserSecurityPreference> findByUserId(String userId);

    List<UserSecurityPreference> findByUserIdAndSection(String userId, String section);

    Optional<UserSecurityPreference> findByUserIdAndSectionAndPreferenceKey(String userId, String section, String preferenceKey);
}
