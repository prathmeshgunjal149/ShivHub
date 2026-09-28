package com.shivhub.backend.repository;

import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import com.shivhub.backend.entity.SellerSubscriptionEntitlement;

public interface SellerSubscriptionEntitlementRepository extends JpaRepository<SellerSubscriptionEntitlement, Long> {
    List<SellerSubscriptionEntitlement> findBySubscriptionId(Long subscriptionId);
    void deleteBySubscriptionId(Long subscriptionId);
}
