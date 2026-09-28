package com.shivhub.backend.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.shivhub.backend.dto.SellerDashboardResponse;
import com.shivhub.backend.service.SellerDashboardService;

/**
 * Seller-only dashboard entry point. Identity is taken from the JWT, preventing
 * a seller from viewing another shop simply by changing a request parameter.
 */
@RestController
@RequestMapping("/api/seller/dashboard")
public class SellerDashboardController {

    private final SellerDashboardService sellerDashboardService;

    public SellerDashboardController(SellerDashboardService sellerDashboardService) {
        this.sellerDashboardService = sellerDashboardService;
    }

    @GetMapping
    public ResponseEntity<SellerDashboardResponse> getDashboard(Authentication authentication) {
        if (authentication == null || authentication.getName() == null) {
            throw new RuntimeException("Authentication required");
        }
        return ResponseEntity.ok(sellerDashboardService.getDashboard(authentication.getName()));
    }
}
