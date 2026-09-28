package com.shivhub.backend.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import com.shivhub.backend.dto.BillingScanErrorResponse;
import com.shivhub.backend.dto.BillingScanResponse;
import com.shivhub.backend.service.BillingScanException;
import com.shivhub.backend.service.BillingScanService;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/seller/offline-billing")
@PreAuthorize("hasRole('SELLER')")
@RequiredArgsConstructor
public class SellerOfflineBillingScanController {
    private final BillingScanService billingScanService;

    @GetMapping("/scan")
    public ResponseEntity<?> scan(Authentication authentication, @RequestParam String code) {
        if (authentication == null || authentication.getName() == null || authentication.getName().isBlank()) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(new BillingScanErrorResponse("Seller session was not found."));
        }
        try {
            return ResponseEntity.ok(billingScanService.scanProductForBilling(authentication.getName(), code));
        } catch (BillingScanException exception) {
            return ResponseEntity.status(exception.getStatus()).body(new BillingScanErrorResponse(exception.getMessage()));
        }
    }
}
