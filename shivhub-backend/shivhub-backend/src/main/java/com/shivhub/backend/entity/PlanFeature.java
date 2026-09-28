package com.shivhub.backend.entity;

import jakarta.persistence.*;
import lombok.Data;

@Entity
@Table(name = "plan_features", uniqueConstraints = @UniqueConstraint(name = "uk_plan_feature", columnNames = { "plan_id", "feature_id" }))
@Data
public class PlanFeature {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY) private Long id;
    @ManyToOne(fetch = FetchType.LAZY, optional = false) @JoinColumn(name = "plan_id", nullable = false) private SubscriptionPlan plan;
    @ManyToOne(fetch = FetchType.LAZY, optional = false) @JoinColumn(name = "feature_id", nullable = false) private SubscriptionFeature feature;
    @Column(nullable = false) private boolean enabled = true;
}
