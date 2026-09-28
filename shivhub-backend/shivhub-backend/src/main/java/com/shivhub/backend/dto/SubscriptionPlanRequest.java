package com.shivhub.backend.dto;

import java.math.BigDecimal;
import java.util.Set;
import com.shivhub.backend.enums.SubscriptionBillingInterval;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record SubscriptionPlanRequest(
        @NotBlank String name, @NotBlank String code, String description, @NotNull BigDecimal price,
        String currency, @NotNull SubscriptionBillingInterval billingInterval, Integer billingIntervalCount,
        Integer trialMonths, boolean active, boolean recommended, Integer displayOrder, Set<String> featureCodes) { }
