package com.shivhub.backend.controller;

import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.util.List;

import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.shivhub.backend.dto.SellerSalesReportResponse;
import com.shivhub.backend.dto.SellerSalesReportRow;
import com.shivhub.backend.service.SellerSalesReportService;

/** Seller report endpoints. Seller identity is always derived from the JWT. */
@RestController
@RequestMapping("/api/seller/reports/sales")
@PreAuthorize("hasRole('SELLER')")
public class SellerSalesReportController {
    private final SellerSalesReportService reportService;
    public SellerSalesReportController(SellerSalesReportService reportService) { this.reportService = reportService; }

    @GetMapping
    public ResponseEntity<SellerSalesReportResponse> getSalesReport(Authentication authentication,
            @RequestParam(required = false) LocalDate startDate, @RequestParam(required = false) LocalDate endDate,
            @RequestParam(required = false) String search, @RequestParam(required = false) Long productId,
            @RequestParam(required = false) String category, @RequestParam(required = false) String paymentMethod,
            @RequestParam(defaultValue = "0") int page, @RequestParam(defaultValue = "20") int size) {
        return ResponseEntity.ok(reportService.getReport(email(authentication), startDate, endDate, search, productId, category, paymentMethod, page, size));
    }

    @GetMapping(value = "/export.csv", produces = "text/csv")
    public ResponseEntity<byte[]> exportCsv(Authentication authentication,
            @RequestParam(required = false) LocalDate startDate, @RequestParam(required = false) LocalDate endDate,
            @RequestParam(required = false) String search, @RequestParam(required = false) Long productId,
            @RequestParam(required = false) String category, @RequestParam(required = false) String paymentMethod) {
        List<SellerSalesReportRow> rows = reportService.getExportRows(email(authentication), startDate, endDate, search, productId, category, paymentMethod);
        StringBuilder csv = new StringBuilder("Source,Reference,Sale Date,Customer,Payment Method,Products,Items,Discount,GST,Total\n");
        for (SellerSalesReportRow row : rows) csv.append(csv(row.source())).append(',').append(csv(row.referenceNumber())).append(',')
                .append(csv(row.saleDate())).append(',').append(csv(row.customerName())).append(',').append(csv(row.paymentMethod())).append(',')
                .append(csv(row.productName())).append(',').append(row.quantity()).append(',').append(row.discount()).append(',')
                .append(row.gst()).append(',').append(row.total()).append('\n');
        return ResponseEntity.ok().header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=seller-sales-report.csv")
                .contentType(new MediaType("text", "csv", StandardCharsets.UTF_8)).body(csv.toString().getBytes(StandardCharsets.UTF_8));
    }

    private String email(Authentication authentication) {
        if (authentication == null || authentication.getName() == null || authentication.getName().isBlank()) throw new RuntimeException("Authentication required");
        return authentication.getName();
    }
    private String csv(Object value) { return "\"" + String.valueOf(value == null ? "" : value).replace("\"", "\"\"") + "\""; }
}
