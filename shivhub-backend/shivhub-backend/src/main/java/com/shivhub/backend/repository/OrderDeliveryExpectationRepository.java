package com.shivhub.backend.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import com.shivhub.backend.entity.OrderDeliveryExpectation;

public interface OrderDeliveryExpectationRepository extends JpaRepository<OrderDeliveryExpectation, Long> {
    List<OrderDeliveryExpectation> findByOrderIdOrderByExpectedDeliveryAtAsc(Long orderId);
    Optional<OrderDeliveryExpectation> findByOrderIdAndSellerId(Long orderId, Long sellerId);
}
