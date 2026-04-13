package com.cloudmen.cloudguard.service;

import com.cloudmen.cloudguard.domain.model.Organization;
import com.cloudmen.cloudguard.domain.model.User;
import com.cloudmen.cloudguard.repository.OrganizationRepository;
import com.cloudmen.cloudguard.repository.UserRepository;
import com.cloudmen.cloudguard.security.WorkspaceIdentityClaims;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Optional;

@Service
public class OrganizationService {

    private final OrganizationRepository organizationRepository;
    private final UserRepository userRepository;

    public OrganizationService(OrganizationRepository organizationRepository, UserRepository userRepository) {
        this.organizationRepository = organizationRepository;
        this.userRepository = userRepository;
    }

    @Transactional
    public void ensureUserLinkedToOrganization(User user, Jwt jwt){
        if(user.getOrganizationId()!=null){
            return;
        }

        Organization org = findOrCreateForCustomerId(jwt.getClaimAsString(WorkspaceIdentityClaims.GOOGLE_WORKSPACE_CUSTOMER_ID));
        user.setOrganizationId(org.getId());
        userRepository.save(user);
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
        o.setName("Organization (no workspace customer id in token)");
        o.setCreatedAt(LocalDateTime.now());
        return organizationRepository.save(o);
    }
}
