package com.shivhub.backend.controller;

import java.util.List;

import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import com.shivhub.backend.dto.CreateOfflineBillRequest;
import com.shivhub.backend.dto.OfflineCustomerLookupResponse;
import com.shivhub.backend.dto.OfflineBillResponse;
import com.shivhub.backend.service.OfflineBillingService;

import jakarta.validation.Valid;


/*
 * =========================================================
 * OfflineBillingController
 * =========================================================
 *
 * Offline / Shop POS Billing APIs.
 *
 * Used by:
 *
 * 1. SELLER
 * 2. ADMIN
 *
 * Main flow:
 *
 * Seller Login
 *      ↓
 * POS Billing
 *      ↓
 * Create Bill
 *      ↓
 * Stock Reduced
 *      ↓
 * Invoice / Bill Response
 *
 * =========================================================
 */

@RestController
@RequestMapping("/api/offline-billing")
public class OfflineBillingController {


    /*
     * =========================================================
     * SERVICE
     * =========================================================
     */

    private final OfflineBillingService offlineBillingService;


    /*
     * =========================================================
     * CONSTRUCTOR
     * =========================================================
     */

    public OfflineBillingController(
            OfflineBillingService offlineBillingService) {

        this.offlineBillingService =
                offlineBillingService;
    }


    /*
     * =========================================================
     * CREATE OFFLINE BILL
     * =========================================================
     *
     * POST:
     *
     * /api/offline-billing
     *
     * SELLER / ADMIN
     *
     * Seller is identified using JWT email.
     *
     * Frontend does NOT send sellerId.
     *
     * =========================================================
     */

    @PostMapping
    @PreAuthorize("hasAnyRole('SELLER', 'ADMIN')")
    public ResponseEntity<OfflineBillResponse> createBill(

            @Valid
            @RequestBody
            CreateOfflineBillRequest request,

            Authentication authentication) {


        /*
         * =====================================================
         * CHECK AUTHENTICATION
         * =====================================================
         */

        if (authentication == null
                || authentication.getName() == null
                || authentication.getName().isBlank()) {

            return ResponseEntity
                    .status(401)
                    .build();
        }


        /*
         * =====================================================
         * GET LOGGED-IN SELLER EMAIL
         * =====================================================
         */

        String sellerEmail =
                authentication.getName();


        /*
         * =====================================================
         * CREATE BILL
         * =====================================================
         */

        OfflineBillResponse response =
                offlineBillingService.createOfflineBill(
                        request,
                        sellerEmail
                );


        return ResponseEntity.ok(response);
    }

    @GetMapping("/salesperson-summary")
    @PreAuthorize("hasRole('SELLER')")
    public ResponseEntity<?> salespersonSummary(Authentication authentication) {
        return ResponseEntity.ok(offlineBillingService.getSalespersonSalesSummary(authentication.getName()));
    }


    /*
     * =========================================================
     * GET ALL BILLS
     * =========================================================
     *
     * GET:
     *
     * /api/offline-billing
     *
     * ADMIN ONLY
     *
     * =========================================================
     */

    @GetMapping
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<List<OfflineBillResponse>>
            getAllBills() {


        List<OfflineBillResponse> bills =
                offlineBillingService.getAllBills();


        return ResponseEntity.ok(bills);
    }


    /*
     * =========================================================
     * GET MY BILLS
     * =========================================================
     *
     * GET:
     *
     * /api/offline-billing/my
     *
     * SELLER ONLY
     *
     * Returns bills created by logged-in seller.
     *
     * =========================================================
     */

    @GetMapping("/my")
    @PreAuthorize("hasRole('SELLER')")
    public ResponseEntity<List<OfflineBillResponse>>
            getMyBills(
                    Authentication authentication) {


        /*
         * =====================================================
         * CHECK AUTHENTICATION
         * =====================================================
         */

        if (authentication == null
                || authentication.getName() == null
                || authentication.getName().isBlank()) {

            return ResponseEntity
                    .status(401)
                    .build();
        }


        /*
         * =====================================================
         * GET SELLER EMAIL
         * =====================================================
         */

        String sellerEmail =
                authentication.getName();


        /*
         * =====================================================
         * GET SELLER BILLS
         * =====================================================
         */

        List<OfflineBillResponse> bills =
                offlineBillingService.getSellerBills(
                        sellerEmail
                );


        return ResponseEntity.ok(bills);
    }


    /*
     * =========================================================
     * GET BILL BY ID
     * =========================================================
     *
     * GET:
     *
     * /api/offline-billing/{id}
     *
     * SELLER / ADMIN
     *
     * =========================================================
     */

    @GetMapping("/{id}")
    @PreAuthorize("hasAnyRole('SELLER', 'ADMIN')")
    public ResponseEntity<OfflineBillResponse>
            getBillById(
                    @PathVariable Long id) {


        OfflineBillResponse bill =
                offlineBillingService.getBillById(
                        id
                );


        return ResponseEntity.ok(bill);
    }


    /*
     * =========================================================
     * GET BILL BY BILL NUMBER
     * =========================================================
     *
     * GET:
     *
     * /api/offline-billing/number/{billNumber}
     *
     * Example:
     *
     * /api/offline-billing/number/
     * SH-BILL-20260901-A1B2C3
     *
     * SELLER / ADMIN
     *
     * =========================================================
     */

    @GetMapping("/number/{billNumber}")
    @PreAuthorize("hasAnyRole('SELLER', 'ADMIN')")
    public ResponseEntity<OfflineBillResponse>
            getBillByNumber(
                    @PathVariable String billNumber) {


        OfflineBillResponse bill =
                offlineBillingService.getBillByNumber(
                        billNumber
                );


        return ResponseEntity.ok(bill);
    }


    /*
     * =========================================================
     * GET CUSTOMER BILLS
     * =========================================================
     *
     * GET:
     *
     * /api/offline-billing/customer/{customerId}
     *
     * ADMIN / SELLER
     *
     * Useful when a registered customer visits the shop
     * and seller wants to see previous offline purchases.
     *
     * =========================================================
     */

    @GetMapping("/customer/{customerId}")
    @PreAuthorize("hasAnyRole('SELLER', 'ADMIN')")
    public ResponseEntity<List<OfflineBillResponse>>
            getCustomerBills(
                    @PathVariable Long customerId) {


        List<OfflineBillResponse> bills =
                offlineBillingService.getCustomerBills(
                        customerId
                );


        return ResponseEntity.ok(bills);
    }

    @GetMapping("/customer/mobile/{mobile}")
    @PreAuthorize("hasAnyRole('SELLER', 'ADMIN')")
    public ResponseEntity<OfflineCustomerLookupResponse>
            findCustomerByMobile(
                    Authentication authentication,
                    @PathVariable String mobile) {

        return ResponseEntity.ok(
                offlineBillingService.findCustomerForBilling(
                        authentication.getName(),
                        mobile
                )
        );
    }
}
