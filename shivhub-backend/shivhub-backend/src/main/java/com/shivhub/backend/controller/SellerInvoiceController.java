package com.shivhub.backend.controller;

import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.shivhub.backend.service.OfflineBillingService;

/** Duplicate-invoice endpoint for a seller's own POS bills. */
@RestController
@RequestMapping("/api/seller/offline-bills")
@PreAuthorize("hasRole('SELLER')")
public class SellerInvoiceController {
    private final OfflineBillingService offlineBillingService;

    public SellerInvoiceController(OfflineBillingService offlineBillingService) {
        this.offlineBillingService = offlineBillingService;
    }

    @GetMapping(value = "/{billId}/invoice.pdf", produces = MediaType.APPLICATION_PDF_VALUE)
    public ResponseEntity<byte[]> printDuplicateInvoice(@PathVariable Long billId, Authentication authentication) {
        if (authentication == null || authentication.getName() == null || authentication.getName().isBlank()) {
            throw new RuntimeException("Authentication required");
        }
        byte[] pdf = offlineBillingService.generateSellerInvoicePdf(billId, authentication.getName());
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "inline; filename=shivhub-bill-" + billId + ".pdf")
                .contentType(MediaType.APPLICATION_PDF)
                .body(pdf);
    }
}
