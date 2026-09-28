package com.shivhub.backend.dto;

import java.math.BigDecimal;

/** Totals calculated from the same filtered rows returned by the report. */
public record SellerSalesReportSummary(
        BigDecimal totalSales,
        long totalBills,
        long totalItemsSold,
        BigDecimal totalGst,
        BigDecimal totalDiscount
) {}
