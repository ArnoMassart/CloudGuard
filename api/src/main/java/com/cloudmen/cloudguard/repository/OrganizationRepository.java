package com.cloudmen.cloudguard.repository;

import com.cloudmen.cloudguard.domain.model.Organization;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface OrganizationRepository extends JpaRepository<Organization,Long> {
    Optional<Organization> findByCustomerId(String customerId);
}
