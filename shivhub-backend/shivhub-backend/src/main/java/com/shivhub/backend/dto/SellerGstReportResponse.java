package com.shivhub.backend.dto;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;

/** Generic, seller-owned reporting response. Monetary fields always come from
 * stored sale/purchase snapshots and are not written back to source records. */
public record SellerGstReportResponse(
        String report,
        LocalDate from,
        LocalDate to,
        Map<String, BigDecimal> totals,
        List<Map<String, Object>> rows,
        long totalRows,
        int page,
        int size,
        int totalPages) { }
