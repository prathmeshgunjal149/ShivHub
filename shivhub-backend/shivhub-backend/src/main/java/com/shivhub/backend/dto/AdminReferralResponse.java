package com.shivhub.backend.dto;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

/** Admin-only referral projection. It deliberately exposes no passwords or unrelated customer data. */
public record AdminReferralResponse(
        List<Row> content,
        long totalElements,
        int totalPages,
        int page,
        int size,
        Summary summary,
        List<TopReferrer> topReferrers) {

    public record Row(
            Long id,
            Customer referrer,
            Customer referredCustomer,
            String referralCode,
            LocalDateTime referralDate,
            String status,
            Coupon rewardCoupon,
            Coupon welcomeCoupon,
            String qualifyingOrderNumber,
            BigDecimal qualifyingOrderRevenue) { }

    public record Customer(Long id, String name, String email, String mobile) { }
    public record Coupon(String code, boolean issued, boolean used, int usedCount) { }
    public record Summary(long total, long pending, long rewarded, long registered) { }
    public record TopReferrer(Long customerId, String name, String email, String mobile, long referralCount, long rewardedCount) { }
}
