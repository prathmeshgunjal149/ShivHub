package com.shivhub.backend.controller;

import java.util.List;

import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.shivhub.backend.dto.ProductCardResponse;
import com.shivhub.backend.dto.ProductRecommendationResponse;
import com.shivhub.backend.service.ProductRecommendationService;
import com.shivhub.backend.service.RecentlyViewedProductService;

@RestController
@RequestMapping
public class ProductRecommendationController {

    private final ProductRecommendationService recommendationService;
    private final RecentlyViewedProductService recentlyViewedService;

    public ProductRecommendationController(
            ProductRecommendationService recommendationService,
            RecentlyViewedProductService recentlyViewedService) {
        this.recommendationService = recommendationService;
        this.recentlyViewedService = recentlyViewedService;
    }

    @GetMapping("/api/products/{productId}/recommendations")
    public ResponseEntity<ProductRecommendationResponse> recommendations(
            @PathVariable Long productId,
            Authentication authentication) {

        List<ProductCardResponse> recent = List.of();
        if (authentication != null && authentication.getName() != null) {
            try {
                recent = recentlyViewedService.getRecent(authentication.getName(), productId);
            } catch (RuntimeException ignored) {
                recent = List.of();
            }
        }

        return ResponseEntity.ok(recommendationService.getRecommendations(productId, recent));
    }

    @PostMapping("/api/customer/recently-viewed/{productId}")
    public ResponseEntity<Void> rememberRecentlyViewed(
            @PathVariable Long productId,
            Authentication authentication) {
        if (authentication == null || authentication.getName() == null) {
            throw new RuntimeException("Authentication required");
        }
        recentlyViewedService.remember(authentication.getName(), productId);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/api/customer/recently-viewed")
    public ResponseEntity<List<ProductCardResponse>> recentlyViewed(
            @RequestParam(required = false) Long excludeProductId,
            Authentication authentication) {
        if (authentication == null || authentication.getName() == null) {
            throw new RuntimeException("Authentication required");
        }
        return ResponseEntity.ok(recentlyViewedService.getRecent(authentication.getName(), excludeProductId));
    }
}
