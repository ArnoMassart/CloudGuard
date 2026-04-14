package com.cloudmen.cloudguard.service;

import com.cloudmen.cloudguard.domain.model.Organization;
import com.cloudmen.cloudguard.domain.model.User;
import com.cloudmen.cloudguard.repository.OrganizationRepository;
import com.cloudmen.cloudguard.repository.UserRepository;
import com.cloudmen.cloudguard.repository.UserSecurityPreferenceRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Service
public class OrganizationService {

    private static final Logger log = LoggerFactory.getLogger(OrganizationService.class);
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
    public void ensureUserLinkedToOrganization(
            User user, String workspaceCustomerId, String workspaceDisplayName) {
        if (workspaceCustomerId != null && !workspaceCustomerId.isBlank()) {
            Organization targetOrg = findOrCreateForCustomerId(workspaceCustomerId, workspaceDisplayName);
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
        }
    }

    public Optional<Organization> findById(Long id) {
        return organizationRepository.findById(id);
    }

    public void updateAdminEmailForOrg(String email, Organization org) {
        if (org.getAdminEmail() == null || org.getAdminEmail().isBlank()) {
            log.info("Eerste Super Admin gedetecteerd voor organisatie {}. Admin email ingesteld op: {}", org.getName(), email);
            org.setAdminEmail(email);
            organizationRepository.save(org);
        }
    }

    private Organization findOrCreateForCustomerId(String customerId, String googleDerivedDisplayName) {
        return organizationRepository
                .findByCustomerId(customerId)
                .map(org -> refreshDisplayNameIfChanged(org, googleDerivedDisplayName))
                .orElseGet(
                        () -> {
                            Organization o = new Organization();
                            o.setCustomerId(customerId);
                            o.setName(googleDerivedDisplayName != null
                                    ? googleDerivedDisplayName
                                    : "Workspace " + customerId);
                            o.setCreatedAt(LocalDateTime.now());
                            return organizationRepository.save(o);
                        });
    }

    private Organization refreshDisplayNameIfChanged(Organization org, String googleDerivedDisplayName) {
        if (googleDerivedDisplayName == null || googleDerivedDisplayName.equals(org.getName())) {
            return org;
        }
        org.setName(googleDerivedDisplayName);
        return organizationRepository.save(org);
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

    public List<Organization> getAll() {
        return organizationRepository.findAll();
    }
}
