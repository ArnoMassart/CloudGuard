package com.cloudmen.cloudguard.integration.repository;

import com.cloudmen.cloudguard.domain.model.Organization;
import com.cloudmen.cloudguard.domain.model.User;
import com.cloudmen.cloudguard.domain.model.UserRole;
import com.cloudmen.cloudguard.repository.OrganizationRepository;
import com.cloudmen.cloudguard.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Real-MySQL integration tests for {@link OrganizationRepository}.
 *
 * <p>Covers the queries that the tenant-management code paths depend on:
 * <ul>
 *   <li>{@code findByCustomerId} — used at login to map a Workspace customer id
 *       to a CloudGuard tenant.</li>
 *   <li>{@code findIdsOfUnusedFallbackOrganizationsWithoutCustomerId} — drives
 *       the cleanup of fallback tenants that no user is pointing at.</li>
 *   <li>{@code findAllWithSearch} — Account beheer ➜ organizations tab.</li>
 *   <li>{@code findAllWithTeamleaderCompanyIdSet} — nightly Teamleader integrity job.</li>
 * </ul>
 */
class OrganizationRepositoryIT extends AbstractRepositoryIT {

    @Autowired
    private OrganizationRepository organizationRepository;

    @Autowired
    private UserRepository userRepository;

    @BeforeEach
    void cleanSlate() {
        userRepository.deleteAll();
        organizationRepository.deleteAll();
    }

    @Test
    @DisplayName("findByCustomerId returns the organization that matches and empty otherwise")
    void findByCustomerId_returnsOrganization() {
        organizationRepository.save(org("Org A", "cust-1", null));
        organizationRepository.save(org("Org B", "cust-2", null));

        Optional<Organization> found = organizationRepository.findByCustomerId("cust-2");
        Optional<Organization> missing = organizationRepository.findByCustomerId("cust-unknown");

        assertThat(found).isPresent().get().extracting(Organization::getName).isEqualTo("Org B");
        assertThat(missing).isEmpty();
    }

    @Test
    @DisplayName("findIdsOfUnusedFallbackOrganizationsWithoutCustomerId only returns orgs with no customerId and no users")
    void findIdsOfUnusedFallback_returnsOnlyUnusedFallbacks() {
        Organization unusedFallbackBlank = organizationRepository.save(org("Unused (blank id)", "", null));
        Organization unusedFallbackNull = organizationRepository.save(org("Unused (null id)", null, null));
        Organization usedFallback = organizationRepository.save(org("Used fallback", null, null));
        Organization realTenant = organizationRepository.save(org("Real tenant", "cust-real", null));

        // Attach a user to `usedFallback` so it should not appear in the result.
        userRepository.save(userInOrg("attached@example.com", usedFallback.getId()));
        // A user on a real tenant must not pull that real tenant into the result either
        userRepository.save(userInOrg("real-user@example.com", realTenant.getId()));

        List<Long> ids = organizationRepository.findIdsOfUnusedFallbackOrganizationsWithoutCustomerId();

        assertThat(ids).containsExactlyInAnyOrder(unusedFallbackBlank.getId(), unusedFallbackNull.getId());
    }

    @Test
    @DisplayName("findAllWithSearch performs a case-insensitive partial match on name and paginates")
    void findAllWithSearch_filtersByName() {
        organizationRepository.save(org("Cloudmen Labs", "cust-1", null));
        organizationRepository.save(org("Cloudmen Production", "cust-2", null));
        organizationRepository.save(org("ACME", "cust-3", null));

        Page<Organization> all = organizationRepository.findAllWithSearch(null, PageRequest.of(0, 20));
        Page<Organization> filtered = organizationRepository.findAllWithSearch("cloud", PageRequest.of(0, 20));
        Page<Organization> none = organizationRepository.findAllWithSearch("zzz", PageRequest.of(0, 20));

        assertThat(all.getTotalElements()).isEqualTo(3);
        assertThat(filtered.getContent())
                .extracting(Organization::getName)
                .containsExactlyInAnyOrder("Cloudmen Labs", "Cloudmen Production");
        assertThat(none.getTotalElements()).isZero();
    }

    @Test
    @DisplayName("findAllWithTeamleaderCompanyIdSet excludes orgs with null or blank teamleader_company_id")
    void findAllWithTeamleaderCompanyIdSet_excludesNullAndBlank() {
        organizationRepository.save(org("With TL", "cust-1", "tl-123"));
        organizationRepository.save(org("Blank TL", "cust-2", "   "));
        organizationRepository.save(org("Null TL", "cust-3", null));

        List<Organization> withTl = organizationRepository.findAllWithTeamleaderCompanyIdSet();

        assertThat(withTl)
                .extracting(Organization::getName)
                .containsExactly("With TL");
    }

    // ---------------------------------------------------------------------
    // Builders
    // ---------------------------------------------------------------------

    private static Organization org(String name, String customerId, String teamleaderCompanyId) {
        Organization o = new Organization();
        o.setName(name);
        o.setCustomerId(customerId);
        o.setTeamleaderCompanyId(teamleaderCompanyId);
        return o;
    }

    private static User userInOrg(String email, Long orgId) {
        User u = new User();
        u.setEmail(email);
        u.setOrganizationId(orgId);
        u.setRoles(new ArrayList<>(List.of(UserRole.UNASSIGNED)));
        u.setAccessAccepted(true);
        return u;
    }
}
