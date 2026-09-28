package com.shivhub.backend.dto;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

/** Seller-scoped payments that are due today or already overdue. */
public record SellerDashboardDueResponse(
        List<DueItem> customerReceivables,
        BigDecimal customerReceivableTotal,
        List<DueItem> distributorPayables,
        BigDecimal distributorPayableTotal
) {
    public record DueItem(
            String partyName,
            String mobile,
            String reference,
            LocalDate dueDate,
            BigDecimal totalAmount,
            BigDecimal paidAmount,
            BigDecimal remainingAmount,
            long overdueDays,
            String status
    ) { }
}
