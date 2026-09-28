package com.shivhub.backend.service;

import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
public class SubscriptionLifecycleScheduler {
    private final SellerEntitlementService subscriptions;
    public SubscriptionLifecycleScheduler(SellerEntitlementService subscriptions) { this.subscriptions = subscriptions; }
    @Scheduled(cron = "${shivhub.subscription.lifecycle-cron:0 10 0 * * *}", zone = "${shivhub.subscription.zone:Asia/Kolkata}")
    public void refreshStatuses() { subscriptions.processDailyLifecycle(); }
}
