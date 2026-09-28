package com.shivhub.backend.dto;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import com.shivhub.backend.enums.ProductStatus;

/** Product data deliberately shaped for dashboard cards; no JPA entity is exposed. */
public record SellerDashboardProductResponse(
        Long id,
        String name,
        String category,
        String imageUrl,
        BigDecimal price,
        Integer stock,
        boolean active,
        ProductStatus approvalStatus,
        LocalDateTime updatedAt
) {
}
