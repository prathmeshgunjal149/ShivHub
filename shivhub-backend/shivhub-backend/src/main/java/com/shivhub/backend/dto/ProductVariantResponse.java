package com.shivhub.backend.dto;
import java.math.BigDecimal;
import java.util.Map;
public record ProductVariantResponse(Long id, String variantSku, Map<String,String> attributes, BigDecimal sellingPriceIncludingGst,
        BigDecimal purchasePrice, BigDecimal compareAtPrice, int availableStock, String imageUrl, boolean active) {}
