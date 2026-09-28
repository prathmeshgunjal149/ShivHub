package com.shivhub.backend.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class ProductReviewRequest {
    @Min(1) @Max(5)
    private Integer rating;
    @NotBlank(message = "Review comment is required")
    private String comment;
}
