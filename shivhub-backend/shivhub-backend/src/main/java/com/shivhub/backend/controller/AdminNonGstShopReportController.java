package com.shivhub.backend.controller;

import java.time.LocalDate;

import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.shivhub.backend.dto.ShopRegisterSummaryResponse;
import com.shivhub.backend.service.SellerShopOperationsService;

/** Read-only admin view. It intentionally stays outside the GST/CA reporting endpoints. */
@RestController
@RequestMapping("/api/admin/non-gst-shop-reports")
@PreAuthorize("hasRole('ADMIN')")
public class AdminNonGstShopReportController {
    private final SellerShopOperationsService service;
    public AdminNonGstShopReportController(SellerShopOperationsService service) { this.service = service; }
    @GetMapping("/{sellerId}/summary") public ShopRegisterSummaryResponse summary(@PathVariable Long sellerId, @RequestParam(required = false) LocalDate date) { return service.adminSummary(sellerId, date); }
}
