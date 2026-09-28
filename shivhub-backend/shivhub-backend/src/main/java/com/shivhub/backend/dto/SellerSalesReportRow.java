package com.shivhub.backend.dto;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/** One seller-owned sale. Online rows are order line-items; POS rows are bills. */
public record SellerSalesReportRow(
        String source,
        Long recordId,
        String referenceNumber,
        LocalDateTime saleDate,
        String customerName,
        String paymentMethod,
        String productName,
        Integer quantity,
        BigDecimal discount,
        BigDecimal taxableAmount,
        BigDecimal cgst,
        BigDecimal sgst,
        BigDecimal igst,
        BigDecimal gst,
        BigDecimal total
) {}
