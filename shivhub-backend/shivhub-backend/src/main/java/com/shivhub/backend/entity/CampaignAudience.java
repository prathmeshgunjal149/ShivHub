package com.shivhub.backend.entity;

import java.time.LocalDateTime;

import jakarta.persistence.*;
import lombok.Data;

/** Immutable audience snapshot so a campaign report always explains who was selected. */
@Entity
@Table(name = "campaign_audiences", uniqueConstraints =
        @UniqueConstraint(name = "uk_campaign_audience_profile", columnNames = {"campaign_id", "customer_profile_id"}))
@Data
public class CampaignAudience {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY) @JoinColumn(name = "campaign_id", nullable = false)
    private MarketingCampaign campaign;

    @ManyToOne(fetch = FetchType.LAZY) @JoinColumn(name = "customer_profile_id", nullable = false)
    private CustomerProfile customerProfile;

    @Column(name = "seller_id")
    private Long sellerId;

    @Column(length = 30)
    private String source; // ONLINE, SELLER, BOTH

    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @PrePersist void created() { createdAt = LocalDateTime.now(); }
}
