package com.shivhub.backend.controller;

import java.time.LocalDate;
import java.util.List;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import com.shivhub.backend.dto.SellerCategorySalesRow;
import com.shivhub.backend.service.SellerCategorySalesReportService;

@RestController @RequestMapping("/api/seller/reports/category-sales") @PreAuthorize("hasRole('SELLER')")
public class SellerCategorySalesReportController {
    private final SellerCategorySalesReportService service;
    public SellerCategorySalesReportController(SellerCategorySalesReportService service) { this.service = service; }
    @GetMapping public ResponseEntity<List<SellerCategorySalesRow>> get(Authentication auth, @RequestParam(required=false) LocalDate startDate, @RequestParam(required=false) LocalDate endDate) {
        if (auth == null || auth.getName() == null || auth.getName().isBlank()) throw new RuntimeException("Authentication required");
        return ResponseEntity.ok(service.get(auth.getName(), startDate, endDate));
    }
}
