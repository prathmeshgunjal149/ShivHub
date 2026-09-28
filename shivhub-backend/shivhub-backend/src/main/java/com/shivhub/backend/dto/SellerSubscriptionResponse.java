package com.shivhub.backend.dto;

import java.time.LocalDate;
import java.util.Map;

public record SellerSubscriptionResponse(Long id, Long sellerId, String sellerName, String shopName,
        SubscriptionPlanResponse currentPlan, String status, LocalDate trialStartDate, LocalDate trialEndDate,
        LocalDate subscriptionStartDate, LocalDate subscriptionEndDate, LocalDate nextBillingDate,
        boolean autoRenewEnabled, long daysRemaining, boolean legacyAccess, Map<String, Boolean> features) { }
