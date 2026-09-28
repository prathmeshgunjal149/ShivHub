package com.shivhub.backend.dto;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import com.shivhub.backend.enums.InstantMobileBillStatus;
import com.shivhub.backend.enums.ShopPaymentMode;

public record InstantMobileBillResponse(
        Long id, String billNumber, InstantMobileBillStatus status, String createdByName,
        String customerName, String customerMobile, String customerEmail, String customerAddress,
        boolean whatsappConsent, String brand, String model, String color, String ram, String storage,
        String imeiOrSerial, Integer quantity, BigDecimal unitPrice, BigDecimal discount,
        BigDecimal totalAmount, ShopPaymentMode paymentMode, String paymentReference, String notes,
        String cancellationReason, String cancelledByName, LocalDateTime cancelledAt, LocalDateTime createdAt) { }
