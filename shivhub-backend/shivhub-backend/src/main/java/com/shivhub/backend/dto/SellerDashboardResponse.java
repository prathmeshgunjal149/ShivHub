package com.shivhub.backend.dto;

import java.time.LocalDateTime;
import java.util.List;

/** Complete response for GET /api/seller/dashboard. */
public record SellerDashboardResponse(
        SellerDashboardSummaryResponse summary,
        List<SellerDashboardProductResponse> recentProducts,
        List<SellerDashboardProductResponse> lowStockProducts,
        SellerDashboardDueResponse todayDues,
        LocalDateTime generatedAt
) {
}
