package com.shivhub.backend.dto;

import java.math.BigDecimal;

/**
 * Small, database-backed summary for the seller landing page.
 * Amounts are returned as BigDecimal so the frontend never loses paise precision.
 */
public record SellerDashboardSummaryResponse(
        long totalProducts,
        long activeProducts,
        long pendingProducts,
        long lowStockProducts,
        long todayOnlineOrders,
        long todayOfflineBills,
        BigDecimal todayOnlineSales,
        BigDecimal todayOfflineSales,
        BigDecimal todayTotalSales
) {
}
