package com.shivhub.backend.entity;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "coupon_usages")
@Data
@NoArgsConstructor
public class CouponUsage {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @Column(nullable = false) private Long couponId;
    @Column(nullable = false) private Long customerId;
    @Column(nullable = false) private Long orderId;
    @Column(nullable = false, precision = 12, scale = 2) private BigDecimal discountAmount;
    @Column(nullable = false) private LocalDateTime usedAt;
}
