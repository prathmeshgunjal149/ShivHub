package com.shivhub.backend.entity;

import java.time.LocalDate;
import java.time.LocalDateTime;
import com.shivhub.backend.enums.SubscriptionStatus;
import jakarta.persistence.*;
import lombok.Data;

@Entity
@Table(name = "seller_subscriptions", uniqueConstraints = @UniqueConstraint(name = "uk_seller_subscription_seller", columnNames = "seller_id"))
@Data
public class SellerSubscription {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY) private Long id;
    @OneToOne(fetch = FetchType.LAZY, optional = false) @JoinColumn(name = "seller_id", nullable = false, unique = true) private User seller;
    @ManyToOne(fetch = FetchType.LAZY) @JoinColumn(name = "current_plan_id") private SubscriptionPlan currentPlan;
    @Column(name = "trial_start_date") private LocalDate trialStartDate;
    @Column(name = "trial_end_date") private LocalDate trialEndDate;
    @Column(name = "subscription_start_date") private LocalDate subscriptionStartDate;
    @Column(name = "subscription_end_date") private LocalDate subscriptionEndDate;
    @Column(name = "next_billing_date") private LocalDate nextBillingDate;
    @Enumerated(EnumType.STRING) @Column(nullable = false, length = 30) private SubscriptionStatus status = SubscriptionStatus.TRIAL;
    @Column(name = "auto_renew_enabled", nullable = false) private boolean autoRenewEnabled;
    @Column(name = "payment_gateway", length = 40) private String paymentGateway;
    @Column(name = "gateway_customer_id", length = 120) private String gatewayCustomerId;
    @Column(name = "gateway_subscription_id", length = 120) private String gatewaySubscriptionId;
    @Column(name = "created_at", nullable = false, updatable = false) private LocalDateTime createdAt;
    @Column(name = "updated_at", nullable = false) private LocalDateTime updatedAt;
    @PrePersist void beforeCreate() { createdAt = LocalDateTime.now(); updatedAt = createdAt; }
    @PreUpdate void beforeUpdate() { updatedAt = LocalDateTime.now(); }
}
