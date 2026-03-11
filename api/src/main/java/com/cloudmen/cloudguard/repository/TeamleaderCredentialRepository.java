package com.cloudmen.cloudguard.repository;

import com.cloudmen.cloudguard.domain.model.TeamleaderCredential;
import org.springframework.data.jpa.repository.JpaRepository;

public interface TeamleaderCredentialRepository extends JpaRepository<TeamleaderCredential, String> {
}
