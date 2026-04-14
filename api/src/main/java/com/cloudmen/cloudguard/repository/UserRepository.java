package com.cloudmen.cloudguard.repository;

import com.cloudmen.cloudguard.domain.model.User;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface UserRepository extends JpaRepository<User, Long> {
    Optional<User> findByEmail(String email);

    @Query("SELECT u FROM tbl_users u WHERE u.roleRequested = :roleRequested " +
            "AND (:query IS NULL OR :query = '' OR " +
            "LOWER(u.firstName) LIKE LOWER(CONCAT('%', :query, '%')) OR " +
            "LOWER(u.lastName) LIKE LOWER(CONCAT('%', :query, '%')) OR " +
            "LOWER(u.email) LIKE LOWER(CONCAT('%', :query, '%')))")
    Page<User> findAllByRoleRequestedWithSearch(
            @Param("roleRequested") boolean roleRequested,
            @Param("query") String query,
            Pageable pageable);

    long countByRoleRequestedTrue();
    boolean existsByOrganizationId(Long organizationId);

    Optional<User> findFirstByOrganizationIdOrderByAsc(Long organizationId);

    @Query("SELECT DISTINCT u.organizationid FROM tbl_users u WHERE u.organizationId IS NOT NULL")
    List<Long> findDistinctOrganizationIds();
}
