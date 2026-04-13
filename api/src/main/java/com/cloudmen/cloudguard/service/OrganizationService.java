package com.cloudmen.cloudguard.service;

import com.cloudmen.cloudguard.domain.model.Organization;
import com.cloudmen.cloudguard.domain.model.User;
import com.cloudmen.cloudguard.repository.OrganizationRepository;
import com.cloudmen.cloudguard.repository.UserRepository;
import com.cloudmen.cloudguard.repository.UserSecurityPreferenceRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Service
public class OrganizationService {

    private final OrganizationRepository organizationRepository;
    private final UserRepository userRepository;
    private final UserSecurityPreferenceRepository userSecurityPreferenceRepository;

    public OrganizationService(
            OrganizationRepository organizationRepository,
            UserRepository userRepository,
            UserSecurityPreferenceRepository userSecurityPreferenceRepository) {
        this.organizationRepository = organizationRepository;
        this.userRepository = userRepository;
        this.userSecurityPreferenceRepository = userSecurityPreferenceRepository;
    }

    @Transactional
    public void ensureUserLinkedToOrganization(User user, String workspaceCustomerId) {
        String trimmed =
                workspaceCustomerId != null && !workspaceCustomerId.isBlank()
                        ? workspaceCustomerId.trim()
                        : null;

        if (trimmed != null) {
            Organization targetOrg = findOrCreateForCustomerId(trimmed);
            Long currentOrgId = user.getOrganizationId();
            if (currentOrgId == null || !currentOrgId.equals(targetOrg.getId())) {
                Long previousOrgId = currentOrgId;
                user.setOrganizationId(targetOrg.getId());
                userRepository.save(user);
                userRepository.flush();
                if (previousOrgId != null) {
                    deleteOrphanFallbackOrganizationIfUnused(previousOrgId);
                }
            }
            deleteAllUnusedFallbackOrganizationsWithoutCustomerId();
            return;
        }

        if (user.getOrganizationId() == null) {
            Organization org = findOrCreateForCustomerId(null);
            user.setOrganizationId(org.getId());
            userRepository.save(user);
        }
    }

    @Transactional
    public Organization findOrCreateForCustomerId(String customerId) {
        if(customerId != null && !customerId.isEmpty()) {
            String key = customerId.trim();
            return organizationRepository.findByCustomerId(key)
                    .orElseGet(
                            () -> {
                                Organization o = new Organization();
                                o.setCustomerId(key);
                                o.setName("Workspace " + key);
                                o.setCreatedAt(LocalDateTime.now());
                                return organizationRepository.save(o);
                            });
        }
        Organization o = new Organization();
        o.setName("Organization (workspace customer id unavailable)");
        o.setCreatedAt(LocalDateTime.now());
        return organizationRepository.save(o);
    }

    /**
     * Removes a workspace-customer-id-unavailable (fallback) organization row once no users reference it,
     * including org-scoped preferences, so relinking to a real tenant does not leave empty orphans.
     */
    private void deleteOrphanFallbackOrganizationIfUnused(Long organizationId) {
        Optional<Organization> orgOpt = organizationRepository.findById(organizationId);
        if (orgOpt.isEmpty()) {
            return;
        }
        Organization org = orgOpt.get();
        if (org.getCustomerId() != null && !org.getCustomerId().isBlank()) {
            return;
        }
        if (userRepository.existsByOrganizationId(organizationId)) {
            return;
        }
        userSecurityPreferenceRepository.deleteByOrganizationId(organizationId);
        organizationRepository.deleteById(organizationId);
    }

    /**
     * Removes every unused fallback organization (no customer id, no linked users). Runs after a
     * successful workspace customer id is known so historical one-org-per-user fallback rows are cleared.
     */
    private void deleteAllUnusedFallbackOrganizationsWithoutCustomerId() {
        List<Long> ids = organizationRepository.findIdsOfUnusedFallbackOrganizationsWithoutCustomerId();
        for (Long id : ids) {
            deleteOrphanFallbackOrganizationIfUnused(id);
        }
    }
}
