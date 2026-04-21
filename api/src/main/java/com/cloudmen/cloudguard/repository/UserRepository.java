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

    @Query("SELECT u FROM tbl_users u WHERE u.roleRequested = true OR u.organizationRequested = true " +
            "AND (:query IS NULL OR :query = '' OR " +
            "LOWER(u.firstName) LIKE LOWER(CONCAT('%', :query, '%')) OR " +
            "LOWER(u.lastName) LIKE LOWER(CONCAT('%', :query, '%')) OR " +
            "LOWER(u.email) LIKE LOWER(CONCAT('%', :query, '%')))")
    Page<User> findAllByRoleRequestedWithSearch(
            @Param("query") String query,
            Pageable pageable);

    @Query("SELECT u FROM tbl_users u WHERE u.roleRequested = false AND u.organizationRequested = false " +
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
}
