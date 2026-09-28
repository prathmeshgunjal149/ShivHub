package com.shivhub.backend.dto;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;

/** Admin-only campaign form. Provider credentials are intentionally not part of this DTO. */
@Data
public class AdminCampaignRequest {
    @NotBlank private String title;
    @NotBlank private String messageType;
    private String festivalName;
    private String subject;
    @NotBlank private String messageContent;
    private String bannerUrl;
    private String couponCode;
    private LocalDateTime validityStartsAt;
    private LocalDateTime validityEndsAt;
    private List<String> channels;
    private List<Long> sellerIds;
    private String customerType; // ONLINE, SELLER, BOTH, ALL
    /** Explicit audience name. customerType is retained for legacy clients. */
    private String targetAudience;
    private String city;
    private Integer inactiveDays;
    private BigDecimal minimumPurchaseAmount;
    private Long productId;
    private String productCategory;
    private String offerDetails;
    private boolean draft = true;
    private boolean sendNow;
    private LocalDateTime scheduledAt;
}
