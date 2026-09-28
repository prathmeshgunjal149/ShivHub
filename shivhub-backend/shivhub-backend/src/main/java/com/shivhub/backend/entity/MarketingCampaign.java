package com.shivhub.backend.entity;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;

/** A reusable admin-managed offer, coupon, banner, greeting, or campaign. */
@Entity
@Table(name = "marketing_campaigns")
@Data
@NoArgsConstructor
public class MarketingCampaign {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @Column(nullable = false, length = 30)
    private String type; // CAMPAIGN, OFFER, COUPON, BANNER, GREETING, REFERRAL
    @Column(name = "message_type", length = 30)
    private String messageType; // OFFER, FESTIVAL, REMINDER, ANNOUNCEMENT
    @Column(nullable = false, length = 20)
    private String audience; // CUSTOMER, SELLER, ALL
    @Column(nullable = false)
    private String title;
    @Column(columnDefinition = "TEXT")
    private String description;
    @Column(name = "festival_name", length = 120)
    private String festivalName;
    @Column(name = "email_subject", length = 250)
    private String emailSubject;
    @Column(name = "banner_url", length = 2000)
    private String bannerUrl;
    /** Optional customer UI slot for an active BANNER campaign. Kept nullable for old campaigns. */
    @Column(name = "placement", length = 40)
    private String placement;
    @Column(name = "target_filters", columnDefinition = "TEXT")
    private String targetFilters;
    @Column(name = "seller_id")
    private Long sellerId;
    @Column(name = "offer_details", columnDefinition = "TEXT")
    private String offerDetails;
    @Column(name = "channels", length = 200)
    private String channels;
    @Column(name = "campaign_status", length = 30)
    private String campaignStatus = "DRAFT";
    @Column(name = "scheduled_at")
    private LocalDateTime scheduledAt;
    @Column(name = "audience_count")
    private Integer audienceCount = 0;
    @Column(name = "sent_count")
    private Integer sentCount = 0;
    @Column(name = "failed_count")
    private Integer failedCount = 0;
    private String couponCode;
    private BigDecimal discountPercent;
    @Enumerated(EnumType.STRING)
    private CouponDiscountType couponDiscountType;
    private BigDecimal discountValue;
    private BigDecimal minimumOrderAmount;
    private BigDecimal maximumDiscount;
    private Integer usageLimit;
    private Integer usedCount = 0;
    private Integer perCustomerLimit = 1;
    /** Present only for a product-specific offer. */
    private Long productId;
    /** When set, this coupon is visible and usable only by this customer. */
    private Long targetCustomerId;
    private boolean active = true;
    private LocalDateTime startsAt;
    private LocalDateTime endsAt;
    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    @PrePersist void create() { createdAt = LocalDateTime.now(); updatedAt = createdAt; }
    @PreUpdate void update() { updatedAt = LocalDateTime.now(); }
}
