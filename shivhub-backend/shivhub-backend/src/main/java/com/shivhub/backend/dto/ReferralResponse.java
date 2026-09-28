package com.shivhub.backend.dto;

import java.time.LocalDateTime;
import lombok.Builder;
import lombok.Data;

@Data @Builder
public class ReferralResponse {
    private String referralCode;
    private String referralLink;
    private long successfulReferrals;
    private java.util.List<ReferralItem> referrals;
    @Data @Builder public static class ReferralItem {
        private String customerName; private String status; private String rewardCouponCode; private LocalDateTime createdAt; private LocalDateTime rewardedAt;
    }
}
