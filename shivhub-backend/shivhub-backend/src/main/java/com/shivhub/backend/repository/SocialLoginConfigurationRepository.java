package com.shivhub.backend.repository;

import com.shivhub.backend.entity.SocialLoginConfiguration;
import org.springframework.data.jpa.repository.JpaRepository;

public interface SocialLoginConfigurationRepository extends JpaRepository<SocialLoginConfiguration, Long> {
}
