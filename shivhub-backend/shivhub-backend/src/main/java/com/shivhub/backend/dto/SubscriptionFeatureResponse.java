package com.shivhub.backend.dto;

import com.shivhub.backend.enums.SubscriptionAccessType;

public record SubscriptionFeatureResponse(Long id, String featureCode, String featureName, String description,
        String category, SubscriptionAccessType accessType, boolean active) { }
