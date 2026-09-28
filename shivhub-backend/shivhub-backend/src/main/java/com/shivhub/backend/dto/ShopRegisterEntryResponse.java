package com.shivhub.backend.dto;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import com.shivhub.backend.enums.ShopPaymentMode;
import com.shivhub.backend.enums.ShopRegisterEntryType;

public record ShopRegisterEntryResponse(
        Long id, ShopRegisterEntryType entryType, ShopPaymentMode paymentMode,
        String category, String itemName, BigDecimal amount, LocalDateTime entryAt,
        String customerName, String customerMobile, boolean whatsappConsent,
        String notes, String createdByName, LocalDateTime createdAt, LocalDateTime updatedAt) { }
