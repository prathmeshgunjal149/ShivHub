package com.shivhub.backend.dto;

public record SubscriptionCheckoutResponse(Long subscriptionPaymentId, String razorpayOrderId, long amount,
        String currency, String keyId, String sellerName, String sellerEmail, String sellerMobile, String description) { }
