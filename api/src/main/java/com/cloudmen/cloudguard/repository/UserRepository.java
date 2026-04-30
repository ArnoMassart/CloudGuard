package com.cloudmen.cloudguard.repository;

import com.cloudmen.cloudguard.domain.model.User;
import com.cloudmen.cloudguard.domain.model.UserRole;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface UserRepository extends JpaRepository<User, Long> {
    Optional<User> findByEmail(String email);

    /**
     * Retrieves a paginated list of users who have pending requests for a new role or organization assignment. <p>
     *
     * The results can be optionally filtered by a search query, which performs a case-insensitive partial match
     * against the user's first name, last name, or email address.
     *
     * @param query     the search string to match against user details
     * @param pageable  pagination and sorting instructions
     * @return a paginated result of users with pending requests
     */
    @Query("SELECT u FROM tbl_users u WHERE u.accessRequested = true " +
            "AND (:query IS NULL OR :query = '' OR " +
            "LOWER(u.firstName) LIKE LOWER(CONCAT('%', :query, '%')) OR " +
            "LOWER(u.lastName) LIKE LOWER(CONCAT('%', :query, '%')) OR " +
            "LOWER(u.email) LIKE LOWER(CONCAT('%', :query, '%')))")
    Page<User> findAllByAccessRequestedWithSearch(
            @Param("query") String query,
            Pageable pageable);

    /**
     * Retrieves a paginated list of standard users who do not have any pending role or organization requests. <p>
     *
     * The results can be narrowed down to a specific organization and optionally filtered by a search query that
     * performs a case-insensitive match on the user's name or email.
     *
     * @param organizationId    the ID of the organization to filter by, or {@code null} to skip this filter
     * @param query             the search string to match against user details
     * @param pageable          pagination and sorting instructions
     * @return a paginated result of users without pending requests
     */
    @Query("SELECT u FROM tbl_users u WHERE u.accessRequested = false " +
            "AND (:organizationId IS NULL OR u.organizationId = :organizationId) " +
            "AND (:query IS NULL OR :query = '' OR " +
            "LOWER(u.firstName) LIKE LOWER(CONCAT('%', :query, '%')) OR " +
            "LOWER(u.lastName) LIKE LOWER(CONCAT('%', :query, '%')) OR " +
            "LOWER(u.email) LIKE LOWER(CONCAT('%', :query, '%')))")
    Page<User> findAllWithoutRequested(
            @Param("organizationId") Long organizationId,
            @Param("query") String query,
            Pageable pageable);

    long countByRoleRequestedTrueOrOrganizationRequestedTrue();
    boolean existsByOrganizationId(Long organizationId);

    Optional<User> findFirstByOrganizationIdOrderByIdAsc(Long organizationId);

    @Query(
            "SELECT u FROM tbl_users u WHERE u.organizationId = :orgId AND :role MEMBER OF u.roles ORDER BY u.id"
                    + " ASC")
    List<User> findByOrganizationIdAndRoleOrderByIdAsc(
            @Param("orgId") Long organizationId, @Param("role") UserRole role);

    @Query("SELECT DISTINCT u.organizationId FROM tbl_users u WHERE u.organizationId IS NOT NULL")
    List<Long> findDistinctOrganizationIds();

    long countByAccessRequestedTrue();
    long countByAccessDeniedTrue();
}
