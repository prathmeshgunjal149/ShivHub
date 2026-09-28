package com.shivhub.backend.dto;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

public record SubscriptionPaymentResponse(Long id, String planName, BigDecimal amount, String currency,
        String paymentStatus, String paymentMethod, String gatewayOrderId, String gatewayPaymentId,
        LocalDate billingPeriodStart, LocalDate billingPeriodEnd, LocalDateTime paidAt, String failureReason,
        LocalDateTime createdAt) { }
