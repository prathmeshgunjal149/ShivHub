package com.shivhub.backend.entity;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import com.shivhub.backend.enums.SubscriptionBillingInterval;
import jakarta.persistence.*;
import lombok.Data;

@Entity
@Table(name = "subscription_plans", uniqueConstraints = @UniqueConstraint(name = "uk_subscription_plan_code", columnNames = "code"))
@Data
public class SubscriptionPlan {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY) private Long id;
    @Column(nullable = false, length = 120) private String name;
    @Column(nullable = false, length = 80) private String code;
    @Column(columnDefinition = "TEXT") private String description;
    @Column(nullable = false, precision = 14, scale = 2) private BigDecimal price = BigDecimal.ZERO;
    @Column(nullable = false, length = 3) private String currency = "INR";
    @Enumerated(EnumType.STRING) @Column(nullable = false, length = 20) private SubscriptionBillingInterval billingInterval = SubscriptionBillingInterval.MONTH;
    @Column(nullable = false) private Integer billingIntervalCount = 1;
    @Column(nullable = false) private Integer trialMonths = 0;
    @Column(nullable = false) private boolean active = true;
    @Column(nullable = false) private boolean recommended;
    @Column(nullable = false) private Integer displayOrder = 0;
    @Column(name = "created_at", nullable = false, updatable = false) private LocalDateTime createdAt;
    @Column(name = "updated_at", nullable = false) private LocalDateTime updatedAt;
    @PrePersist void beforeCreate() { createdAt = LocalDateTime.now(); updatedAt = createdAt; }
    @PreUpdate void beforeUpdate() { updatedAt = LocalDateTime.now(); }
}
