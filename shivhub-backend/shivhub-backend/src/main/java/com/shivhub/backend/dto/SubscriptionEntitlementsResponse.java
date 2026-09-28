package com.shivhub.backend.dto;

import java.util.Map;

public record SubscriptionEntitlementsResponse(String subscriptionStatus, boolean trialActive,
        boolean renewalRequired, boolean legacyAccess, Map<String, Boolean> features) { }
