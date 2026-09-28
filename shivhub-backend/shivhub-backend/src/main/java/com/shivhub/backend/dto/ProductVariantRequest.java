package com.shivhub.backend.dto;
import java.math.BigDecimal;
import java.util.Map;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
public record ProductVariantRequest(String variantSku, String barcode, @NotEmpty Map<String,String> attributes,
        @NotNull BigDecimal sellingPriceIncludingGst, BigDecimal purchasePrice, BigDecimal compareAtPrice, @NotNull Integer stockQuantity,
        String imageUrl, Boolean active, Integer reorderThreshold, Long id, Long version) {}
