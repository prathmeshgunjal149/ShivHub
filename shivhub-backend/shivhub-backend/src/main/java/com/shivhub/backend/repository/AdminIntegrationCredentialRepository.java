package com.shivhub.backend.repository;

import com.shivhub.backend.entity.AdminIntegrationCredential;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface AdminIntegrationCredentialRepository extends JpaRepository<AdminIntegrationCredential, Long> {
    Optional<AdminIntegrationCredential> findByIntegration(String integration);
}
