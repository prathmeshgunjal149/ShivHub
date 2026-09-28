package com.shivhub.backend.dto;

import java.math.BigDecimal;
import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class ProductReviewSummary {
    private BigDecimal averageRating;
    private long totalReviews;
    private long[] ratingBreakdown;
    private List<ProductReviewResponse> reviews;
}
