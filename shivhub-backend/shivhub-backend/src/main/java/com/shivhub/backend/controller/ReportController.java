package com.shivhub.backend.controller;

import java.util.Map;
import java.time.LocalDate;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import com.shivhub.backend.service.ReportService;

@RestController
@RequestMapping("/api/admin/reports")
@PreAuthorize("hasRole('ADMIN')")
public class ReportController {
    private final ReportService reportService;
    public ReportController(ReportService reportService) { this.reportService = reportService; }
    @GetMapping("/summary")
    public Map<String, Object> summary() { return reportService.summary(); }

    @GetMapping("/accounting")
    public Map<String, Object> accounting() { return reportService.accounting(); }

    @GetMapping("/seller-performance")
    public Map<String, Object> sellerPerformance(
            @RequestParam(required = false) LocalDate startDate,
            @RequestParam(required = false) LocalDate endDate) {
        return reportService.sellerPerformance(startDate, endDate);
    }
}
