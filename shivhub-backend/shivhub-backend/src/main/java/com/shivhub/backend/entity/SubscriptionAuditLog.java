package com.shivhub.backend.entity;

import java.time.LocalDateTime;
import jakarta.persistence.*;
import lombok.Data;

@Entity
@Table(name = "subscription_audit_logs")
@Data
public class SubscriptionAuditLog {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY) private Long id;
    @ManyToOne(fetch = FetchType.LAZY, optional = false) @JoinColumn(name = "seller_subscription_id", nullable = false) private SellerSubscription subscription;
    @Column(name = "actor_user_id") private Long actorUserId;
    @Column(name = "event_type", nullable = false, length = 80) private String eventType;
    @Column(name = "old_value", columnDefinition = "TEXT") private String oldValue;
    @Column(name = "new_value", columnDefinition = "TEXT") private String newValue;
    @Column(columnDefinition = "TEXT") private String reason;
    @Column(name = "created_at", nullable = false, updatable = false) private LocalDateTime createdAt;
    @PrePersist void beforeCreate() { createdAt = LocalDateTime.now(); }
}
