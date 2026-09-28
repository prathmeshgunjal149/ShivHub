package com.shivhub.backend.entity;

import java.time.LocalDateTime;

import jakarta.persistence.*;
import lombok.Data;

/** Owner-scoped relationship used to enforce seller customer-data isolation. */
@Entity
@Table(name = "seller_customer_mappings", uniqueConstraints =
        @UniqueConstraint(name = "uk_seller_customer_profile", columnNames = {"seller_id", "customer_profile_id"}))
@Data
public class SellerCustomerMapping {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY) @JoinColumn(name = "seller_id", nullable = false)
    private User seller;

    @ManyToOne(fetch = FetchType.LAZY) @JoinColumn(name = "customer_profile_id", nullable = false)
    private CustomerProfile customerProfile;

    @Column(nullable = false, updatable = false)
    private LocalDateTime firstPurchaseAt;

    private LocalDateTime lastPurchaseAt;

    @PrePersist void created() {
        if (firstPurchaseAt == null) firstPurchaseAt = LocalDateTime.now();
        if (lastPurchaseAt == null) lastPurchaseAt = firstPurchaseAt;
    }
}
