package com.shivhub.backend.service;

import com.shivhub.backend.enums.SubscriptionStatus;

public class SubscriptionRequiredException extends RuntimeException {
    private final SubscriptionStatus status;
    public SubscriptionRequiredException(SubscriptionStatus status) { super("This feature requires an active ShivHub subscription."); this.status = status; }
    public SubscriptionStatus getStatus() { return status; }
}
