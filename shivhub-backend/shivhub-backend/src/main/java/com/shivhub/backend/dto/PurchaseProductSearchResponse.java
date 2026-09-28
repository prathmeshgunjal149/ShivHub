package com.shivhub.backend.dto;

import java.math.BigDecimal;
import java.util.Map;

/** Safe, compact product data used by the seller purchase search. */
public record PurchaseProductSearchResponse(
        Long id,
        String name,
        String brand,
        String model,
        String ram,
        String storage,
        String colorOptions,
        String sellerSku,
        String barcode,
        String hsnCode,
        BigDecimal purchasePrice,
        BigDecimal sellingPrice,
        BigDecimal gstRate,
        Integer stock,
        Long categoryId,
        String categoryName,
        Long subCategoryId,
        String subCategoryName,
        boolean mobile,
        boolean variantsEnabled,
        Map<String, String> productSpecifications
) { }
