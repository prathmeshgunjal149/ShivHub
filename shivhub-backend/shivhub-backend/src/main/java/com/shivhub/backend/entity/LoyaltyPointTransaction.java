package com.shivhub.backend.entity;

import java.time.LocalDateTime;
import jakarta.persistence.*;
import lombok.Data;

/** Immutable customer-points ledger. Positive and negative point values are both explicit. */
@Entity
@Table(name = "loyalty_point_transactions", indexes = {
        @Index(name = "idx_loyalty_transaction_customer", columnList = "customer_profile_id,created_at"),
        @Index(name = "idx_loyalty_transaction_seller", columnList = "seller_id,created_at")
}, uniqueConstraints = @UniqueConstraint(name = "uk_loyalty_source_type_transaction",
        columnNames = {"source_type", "source_id", "transaction_type"}))
@Data
public class LoyaltyPointTransaction {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "customer_profile_id", nullable = false)
    private CustomerProfile customerProfile;
    @Column(name = "transaction_type", nullable = false, length = 16)
    private String transactionType; // EARN, REDEEM, REVERSE, ADJUST
    @Column(name = "source_type", nullable = false, length = 24)
    private String sourceType; // OFFLINE_BILL, ONLINE_ORDER, ADMIN
    @Column(name = "source_id", nullable = false)
    private Long sourceId;
    @Column(name = "seller_id")
    private Long sellerId;
    @Column(name = "shop_id")
    private Long shopId;
    @Column(nullable = false)
    private long points;
    @Column(name = "balance_after", nullable = false)
    private long balanceAfter;
    @Column(columnDefinition = "TEXT")
    private String remarks;
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;
    @PrePersist void create() { createdAt = LocalDateTime.now(); }
}
