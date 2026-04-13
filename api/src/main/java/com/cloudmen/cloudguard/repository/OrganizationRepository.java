package com.cloudmen.cloudguard.repository;

import com.cloudmen.cloudguard.domain.model.Organization;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;
import java.util.Optional;

public interface OrganizationRepository extends JpaRepository<Organization,Long> {
    Optional<Organization> findByCustomerId(String customerId);

    /**
     * Fallback tenants have no workspace customer id; unused rows have no users pointing at them.
     */
    @Query(
            """
            SELECT o.id FROM Organization o
            WHERE (o.customerId IS NULL OR o.customerId = '')
            AND NOT EXISTS (SELECT 1 FROM tbl_users u WHERE u.organizationId = o.id)
            """)
    List<Long> findIdsOfUnusedFallbackOrganizationsWithoutCustomerId();
}
