package com.shivhub.backend.dto;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

/**
 * Safe, invoice-level view used by the admin purchase approval queue.
 * It intentionally contains the supplier, receiving seller and invoice
 * snapshots needed for a decision without exposing unrelated seller data.
 */
public record AdminPurchaseApprovalResponse(
        Long purchaseId,
        String invoiceNumber,
        LocalDateTime purchaseDate,
        LocalDateTime submittedAt,
        String sellerName,
        String sellerEmail,
        String sellerMobile,
        String supplierName,
        String supplierGstin,
        String supplierContact,
        BigDecimal subtotal,
        BigDecimal gstTotal,
        BigDecimal discount,
        BigDecimal grandTotal,
        String invoiceFileUrl,
        String notes,
        int pendingProductCount,
        List<Item> items) {

    public record Item(
            Long productId,
            String productName,
            String sku,
            String brand,
            String model,
            String color,
            String ram,
            String storage,
            Integer quantity,
            String unit,
            BigDecimal unitPrice,
            BigDecimal totalPrice,
            boolean pendingProductApproval) {
    }
}
