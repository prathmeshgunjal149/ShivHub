package com.shivhub.backend.controller;

import java.util.List;
import java.util.Map;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.security.core.Authentication;

import com.shivhub.backend.dto.PurchasePaymentRequest;
import com.shivhub.backend.dto.PurchasePaymentSummaryResponse;
import com.shivhub.backend.dto.DistributorPayableBillResponse;
import com.shivhub.backend.entity.PurchasePayment;
import com.shivhub.backend.entity.User;
import com.shivhub.backend.repository.UserRepository;
import com.shivhub.backend.service.PurchasePaymentService;

import lombok.RequiredArgsConstructor;


/*
 * =========================================================
 * PURCHASE PAYMENT CONTROLLER
 * =========================================================
 *
 * Handles Seller → Distributor payment APIs.
 *
 * =========================================================
 */

@RestController
@RequestMapping("/api/purchase-payments")
@RequiredArgsConstructor
@CrossOrigin(origins = "*")
public class PurchasePaymentController {


    private final PurchasePaymentService purchasePaymentService;

    private final UserRepository userRepository;

    @GetMapping("/payable-bills")
    public ResponseEntity<?> getPayableBills(Authentication authentication) {
        try {
            User seller = userRepository.findByEmail(authentication.getName())
                    .orElseThrow(() -> new RuntimeException("Seller not found"));
            List<DistributorPayableBillResponse> bills = purchasePaymentService.getPayableBills(seller);
            return ResponseEntity.ok(bills);
        } catch (RuntimeException exception) {
            return ResponseEntity.badRequest().body(Map.of("message", exception.getMessage()));
        }
    }


    /*
     * =========================================================
     * ADD PAYMENT
     * =========================================================
     *
     * POST:
     *
     * /api/purchase-payments
     *
     * Body:
     *
     * {
     *   "purchaseId": 1,
     *   "amount": 50000,
     *   "paymentMethod": "UPI",
     *   "paymentDate": "2026-09-03T14:00:00",
     *   "transactionReference": "UPI123456",
     *   "notes": "First payment"
     * }
     *
     * =========================================================
     */

    @PostMapping
    public ResponseEntity<?> addPayment(
            Authentication authentication,
            @RequestBody PurchasePaymentRequest request) {

        try {

            User seller =
                    userRepository
                            .findByEmail(authentication.getName())
                            .orElseThrow(
                                    () -> new RuntimeException(
                                            "Seller not found"
                                    )
                            );


            PurchasePayment payment =
                    purchasePaymentService.addPayment(
                            request,
                            seller
                    );


            return ResponseEntity
                    .status(HttpStatus.CREATED)
                    .body(payment);


        } catch (RuntimeException exception) {

            return ResponseEntity
                    .badRequest()
                    .body(
                            Map.of(
                                    "message",
                                    exception.getMessage()
                            )
                    );
        }
    }


    /*
     * =========================================================
     * GET PURCHASE PAYMENTS
     * =========================================================
     *
     * GET:
     *
     * /api/purchase-payments/purchase/1
     *
     * =========================================================
     */

    @GetMapping("/purchase/{purchaseId}")
    public ResponseEntity<?> getPayments(
            @PathVariable Long purchaseId,
            Authentication authentication) {

        try {

            User seller =
                    userRepository
                            .findByEmail(authentication.getName())
                            .orElseThrow(
                                    () -> new RuntimeException(
                                            "Seller not found"
                                    )
                            );


            List<PurchasePayment> payments =
                    purchasePaymentService.getPayments(
                            purchaseId,
                            seller
                    );


            return ResponseEntity.ok(
                    payments
            );


        } catch (RuntimeException exception) {

            return ResponseEntity
                    .badRequest()
                    .body(
                            Map.of(
                                    "message",
                                    exception.getMessage()
                            )
                    );
        }
    }


    /*
     * =========================================================
     * GET PAYMENT SUMMARY
     * =========================================================
     *
     * GET:
     *
     * /api/purchase-payments/purchase/1/summary
     *
     * Response:
     *
     * {
     *   "purchaseTotal": 590000,
     *   "totalPaid": 100000,
     *   "remainingAmount": 490000,
     *   "paymentStatus": "PARTIALLY_PAID"
     * }
     *
     * =========================================================
     */

    @GetMapping("/purchase/{purchaseId}/summary")
    public ResponseEntity<?> getSummary(
            @PathVariable Long purchaseId,
            Authentication authentication) {

        try {

            User seller =
                    userRepository
                            .findByEmail(authentication.getName())
                            .orElseThrow(
                                    () -> new RuntimeException(
                                            "Seller not found"
                                    )
                            );


            PurchasePaymentSummaryResponse summary =
                    purchasePaymentService.getSummary(
                            purchaseId,
                            seller
                    );


            return ResponseEntity.ok(
                    summary
            );


        } catch (RuntimeException exception) {

            return ResponseEntity
                    .badRequest()
                    .body(
                            Map.of(
                                    "message",
                                    exception.getMessage()
                            )
                    );
        }
    }
}
