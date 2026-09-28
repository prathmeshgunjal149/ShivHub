package com.shivhub.backend.dto;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/** Real-time seller roll-up assembled from shops, products, bills, orders and receivables. */
public record AdminSellerSummaryResponse(
        Long id, String ownerName, String shopName, String shopLogoUrl,
        String email, String mobile, String address, String city, String gstin,
        String approvalStatus, boolean active, boolean gstVerified,
        int totalProducts, int availableStockQuantity, BigDecimal stockValue,
        BigDecimal offlineSales, BigDecimal onlineSales, long totalOrders,
        BigDecimal totalRevenue, BigDecimal pendingDistributorPayment,
        BigDecimal customerReceivable, long totalCustomers,
        LocalDateTime registrationDate, LocalDateTime lastLoginAt
) { }
