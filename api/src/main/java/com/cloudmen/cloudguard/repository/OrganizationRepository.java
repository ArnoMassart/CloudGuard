package com.cloudmen.cloudguard.repository;

import com.cloudmen.cloudguard.domain.model.Organization;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

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

    /**
     * Retrieves a paginated list of organizations, optionally filtered by a search query. <p>
     *
     * If the query is null or empty, all organizations are returned. Otherwise, it performs a case-insensitive
     * partial match on the organization's name.
     *
     * @param query     the search string to match against names
     * @param pageable  pagination and sorting instructions
     * @return a paginated result of matched organizations
     */
    @Query("SELECT o FROM Organization o WHERE " +
            ":query IS NULL OR :query = '' OR " +
            "LOWER(o.name) LIKE LOWER(CONCAT('%', :query, '%'))")
    Page<Organization> findAllWithSearch(
            @Param("query") String query,
            Pageable pageable);

    @Query("SELECT o FROM Organization o WHERE o.teamleaderCompanyId IS NOT NULL AND TRIM(o.teamleaderCompanyId) <> ''")
    List<Organization> findAllWithTeamleaderCompanyIdSet();
}
