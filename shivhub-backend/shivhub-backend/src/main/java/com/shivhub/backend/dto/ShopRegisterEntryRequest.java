package com.shivhub.backend.dto;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import com.shivhub.backend.enums.ShopPaymentMode;
import com.shivhub.backend.enums.ShopRegisterEntryType;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class ShopRegisterEntryRequest {
    @NotNull private ShopRegisterEntryType entryType;
    @NotNull private ShopPaymentMode paymentMode;
    @NotBlank private String category;
    private String itemName;
    @NotNull private BigDecimal amount;
    private LocalDateTime entryAt;
    private String customerName;
    private String customerMobile;
    private boolean whatsappConsent;
    private String notes;
    /** Required for an edit/delete audit, optional on a new entry. */
    private String reason;
}
