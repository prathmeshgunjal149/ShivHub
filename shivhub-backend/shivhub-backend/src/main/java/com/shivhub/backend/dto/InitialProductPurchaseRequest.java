package com.shivhub.backend.dto;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

/** Genuine procurement details for opening serialized non-mobile stock. */
public record InitialProductPurchaseRequest(Long sellerDistributorId, String invoiceNumber,
        LocalDateTime purchaseDate, BigDecimal unitPrice, BigDecimal gstRate,
        List<PurchaseItemSerialRequest> serials) {}
