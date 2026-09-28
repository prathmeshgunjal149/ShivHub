package com.shivhub.backend.dto;

import java.math.BigDecimal;

public record SellerCategorySalesRow(String category, long onlineQuantity,
        long posQuantity, long totalQuantity, BigDecimal totalSales) {}
