package com.shivhub.backend.dto;

import java.math.BigDecimal;

public record ProductCardResponse(
        Long id,
        String name,
        String brand,
        String model,
        String ram,
        String storage,
        String colorOptions,
        String hsnCode,
        BigDecimal price,
        BigDecimal finalPrice,
        BigDecimal offerPercentage,
        Integer stock,
        Integer availableStock,
        boolean available,
        String imageUrl,
        String category,
        String subCategory,
        String sellerName,
        BigDecimal averageRating,
        long reviewCount
) {
}
