package com.shivhub.backend.entity;

import com.shivhub.backend.enums.SubscriptionAccessType;
import jakarta.persistence.*;
import lombok.Data;

@Entity
@Table(name = "subscription_features", uniqueConstraints = @UniqueConstraint(name = "uk_subscription_feature_code", columnNames = "feature_code"))
@Data
public class SubscriptionFeature {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY) private Long id;
    @Column(name = "feature_code", nullable = false, length = 100) private String featureCode;
    @Column(name = "feature_name", nullable = false, length = 160) private String featureName;
    @Column(columnDefinition = "TEXT") private String description;
    @Column(length = 80) private String category;
    @Enumerated(EnumType.STRING) @Column(name = "access_type", nullable = false, length = 30) private SubscriptionAccessType accessType = SubscriptionAccessType.SUBSCRIPTION;
    @Column(nullable = false) private boolean active = true;
}
