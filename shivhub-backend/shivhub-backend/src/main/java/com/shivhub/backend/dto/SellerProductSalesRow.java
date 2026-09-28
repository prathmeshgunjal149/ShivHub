package com.shivhub.backend.dto;

import java.math.BigDecimal;

/** Product totals use immutable sale-time item values from online and POS sales. */
public record SellerProductSalesRow(Long productId, String productName, String category,
        long onlineQuantity, long posQuantity, long totalQuantity, BigDecimal totalSales) {}
