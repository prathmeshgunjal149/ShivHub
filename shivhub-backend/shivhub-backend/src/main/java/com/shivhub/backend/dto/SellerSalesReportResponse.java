package com.shivhub.backend.dto;

import java.time.LocalDate;
import java.util.List;

public record SellerSalesReportResponse(
        LocalDate startDate,
        LocalDate endDate,
        SellerSalesReportSummary summary,
        List<SellerSalesReportRow> rows,
        long totalRows,
        int page,
        int size,
        int totalPages
) {}
