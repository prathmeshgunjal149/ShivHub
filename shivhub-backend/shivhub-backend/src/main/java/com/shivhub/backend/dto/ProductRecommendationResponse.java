package com.shivhub.backend.dto;

import java.util.List;

public record ProductRecommendationResponse(
        String frequentlyBoughtLabel,
        String frequentlyBoughtSource,
        List<ProductCardResponse> frequentlyBoughtTogether,
        String compatibleAccessoriesLabel,
        List<ProductCardResponse> compatibleAccessories,
        String similarProductsLabel,
        List<ProductCardResponse> similarProducts,
        String recentlyViewedLabel,
        List<ProductCardResponse> recentlyViewed
) {
}
