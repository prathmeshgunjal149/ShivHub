package com.shivhub.backend.entity;

import java.time.LocalDateTime;

import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;

/** Audit record for one customer referral and its reward lifecycle. */
@Entity
@Table(name = "referrals", uniqueConstraints = @UniqueConstraint(columnNames = "referred_customer_id"))
@Data
@NoArgsConstructor
public class Referral {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @Column(name = "referrer_customer_id", nullable = false) private Long referrerCustomerId;
    @Column(name = "referred_customer_id", nullable = false) private Long referredCustomerId;
    @Column(nullable = false, length = 20) private String status = "PENDING";
    private Long qualifyingOrderId;
    private String referrerCouponCode;
    private String referredCouponCode;
    @Column(nullable = false, updatable = false) private LocalDateTime createdAt;
    private LocalDateTime rewardedAt;
    @PrePersist void created() { createdAt = LocalDateTime.now(); }
}
