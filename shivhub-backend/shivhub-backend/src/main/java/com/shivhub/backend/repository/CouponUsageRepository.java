package com.shivhub.backend.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import com.shivhub.backend.entity.CouponUsage;

public interface CouponUsageRepository extends JpaRepository<CouponUsage, Long> {
    long countByCouponIdAndCustomerId(Long couponId, Long customerId);
}
