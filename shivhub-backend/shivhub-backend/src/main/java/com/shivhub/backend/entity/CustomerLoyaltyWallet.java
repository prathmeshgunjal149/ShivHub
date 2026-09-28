package com.shivhub.backend.entity;

import java.time.LocalDateTime;
import jakarta.persistence.*;
import lombok.Data;

/** One balance per customer identity; balances are changed only through ledger rows. */
@Entity
@Table(name = "customer_loyalty_wallet", uniqueConstraints =
        @UniqueConstraint(name = "uk_loyalty_wallet_customer", columnNames = "customer_profile_id"))
@Data
public class CustomerLoyaltyWallet {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "customer_profile_id", nullable = false)
    private CustomerProfile customerProfile;

    @Column(name = "available_points", nullable = false)
    private long availablePoints;
    @Column(name = "lifetime_earned_points", nullable = false)
    private long lifetimeEarnedPoints;
    @Column(name = "lifetime_redeemed_points", nullable = false)
    private long lifetimeRedeemedPoints;
    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    @PrePersist @PreUpdate
    void touch() { updatedAt = LocalDateTime.now(); }
}
