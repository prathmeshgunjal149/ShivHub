package com.shivhub.backend.entity;

import jakarta.persistence.*;
import lombok.Data;

/** Snapshot: plan edits must not change features already purchased for the current period. */
@Entity
@Table(name = "seller_subscription_entitlements", uniqueConstraints = @UniqueConstraint(name = "uk_subscription_entitlement", columnNames = { "seller_subscription_id", "feature_code" }))
@Data
public class SellerSubscriptionEntitlement {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY) private Long id;
    @ManyToOne(fetch = FetchType.LAZY, optional = false) @JoinColumn(name = "seller_subscription_id", nullable = false) private SellerSubscription subscription;
    @Column(name = "feature_code", nullable = false, length = 100) private String featureCode;
    @Column(nullable = false) private boolean enabled;
}
