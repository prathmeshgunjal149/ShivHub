package com.shivhub.backend.dto;

import java.math.BigDecimal;

/** Immutable POS performance total for a salesperson/staff member. */
public record SalespersonSalesSummaryResponse(
        Long salespersonId,
        String salespersonName,
        long billCount,
        long mobileUnitsSold,
        BigDecimal salesTotal,
        BigDecimal discountTotal,
        BigDecimal gstTotal,
        BigDecimal netTotal,
        BigDecimal profitEstimate,
        BigDecimal marginPercent
) {}
