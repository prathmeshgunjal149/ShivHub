package com.shivhub.backend.repository;

import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import com.shivhub.backend.entity.SubscriptionPayment;
import com.shivhub.backend.enums.SubscriptionPaymentStatus;

public interface SubscriptionPaymentRepository extends JpaRepository<SubscriptionPayment, Long> {
    List<SubscriptionPayment> findBySellerIdOrderByCreatedAtDesc(Long sellerId);
    List<SubscriptionPayment> findAllByOrderByCreatedAtDesc();
    Optional<SubscriptionPayment> findByGatewayOrderId(String gatewayOrderId);
    Optional<SubscriptionPayment> findByGatewayPaymentId(String gatewayPaymentId);
    long countByPaymentStatus(SubscriptionPaymentStatus status);
}
