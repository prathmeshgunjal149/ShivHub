package com.shivhub.backend.repository;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import com.shivhub.backend.entity.PolicyPage;

public interface PolicyPageRepository extends JpaRepository<PolicyPage, Long> {
    Optional<PolicyPage> findBySlug(String slug);
    boolean existsBySlug(String slug);
}
