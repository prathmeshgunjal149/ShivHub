package com.shivhub.backend.dto;

import java.math.BigDecimal;
import java.util.List;
import com.shivhub.backend.enums.SubscriptionBillingInterval;

public record SubscriptionPlanResponse(Long id, String name, String code, String description, BigDecimal price,
        String currency, SubscriptionBillingInterval billingInterval, Integer billingIntervalCount,
        Integer trialMonths, boolean active, boolean recommended, Integer displayOrder, List<String> featureCodes) { }
