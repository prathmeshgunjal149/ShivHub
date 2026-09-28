package com.shivhub.backend.entity;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import com.shivhub.backend.enums.SubscriptionPaymentStatus;
import jakarta.persistence.*;
import lombok.Data;

@Entity
@Table(name = "subscription_payments", uniqueConstraints = {
        @UniqueConstraint(name = "uk_subscription_gateway_order", columnNames = "gateway_order_id"),
        @UniqueConstraint(name = "uk_subscription_gateway_payment", columnNames = "gateway_payment_id") })
@Data
public class SubscriptionPayment {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY) private Long id;
    @ManyToOne(fetch = FetchType.LAZY, optional = false) @JoinColumn(name = "seller_id", nullable = false) private User seller;
    @ManyToOne(fetch = FetchType.LAZY, optional = false) @JoinColumn(name = "seller_subscription_id", nullable = false) private SellerSubscription sellerSubscription;
    @ManyToOne(fetch = FetchType.LAZY, optional = false) @JoinColumn(name = "plan_id", nullable = false) private SubscriptionPlan plan;
    @Column(nullable = false, precision = 14, scale = 2) private BigDecimal amount;
    @Column(nullable = false, length = 3) private String currency = "INR";
    @Enumerated(EnumType.STRING) @Column(name = "payment_status", nullable = false, length = 30) private SubscriptionPaymentStatus paymentStatus = SubscriptionPaymentStatus.CREATED;
    @Column(name = "payment_method", length = 40) private String paymentMethod;
    @Column(name = "gateway_order_id", length = 120, unique = true) private String gatewayOrderId;
    @Column(name = "gateway_payment_id", length = 120, unique = true) private String gatewayPaymentId;
    @Column(name = "gateway_signature", length = 255) private String gatewaySignature;
    @Column(name = "billing_period_start") private LocalDate billingPeriodStart;
    @Column(name = "billing_period_end") private LocalDate billingPeriodEnd;
    @Column(name = "paid_at") private LocalDateTime paidAt;
    @Column(name = "failure_reason", length = 500) private String failureReason;
    @Column(name = "created_at", nullable = false, updatable = false) private LocalDateTime createdAt;
    @Column(name = "updated_at", nullable = false) private LocalDateTime updatedAt;
    @PrePersist void beforeCreate() { createdAt = LocalDateTime.now(); updatedAt = createdAt; }
    @PreUpdate void beforeUpdate() { updatedAt = LocalDateTime.now(); }
}
