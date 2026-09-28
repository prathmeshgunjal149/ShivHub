package com.shivhub.backend.repository;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import com.shivhub.backend.entity.SellerRegistrationOtp;

public interface SellerRegistrationOtpRepository extends JpaRepository<SellerRegistrationOtp, Long> {

    Optional<SellerRegistrationOtp> findTopByEmailIgnoreCaseOrderByCreatedAtDesc(String email);

    boolean existsByEmailIgnoreCaseAndVerifiedFalse(String email);

    boolean existsByMobileAndVerifiedFalse(String mobile);
}
