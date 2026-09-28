package com.shivhub.backend.repository;

import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import com.shivhub.backend.entity.Referral;

public interface ReferralRepository extends JpaRepository<Referral, Long> {
    Optional<Referral> findByReferredCustomerId(Long customerId);
    List<Referral> findByReferrerCustomerIdOrderByCreatedAtDesc(Long customerId);
    List<Referral> findAllByOrderByCreatedAtDesc();
}
