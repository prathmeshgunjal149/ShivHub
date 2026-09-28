package com.shivhub.backend.controller;

import java.util.List;
import java.util.Map;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import com.shivhub.backend.dto.PurchaseRequest;
import com.shivhub.backend.entity.Purchase;
import com.shivhub.backend.entity.PurchaseItem;
import com.shivhub.backend.entity.User;
import com.shivhub.backend.repository.PurchaseRepository;
import com.shivhub.backend.repository.UserRepository;
import com.shivhub.backend.service.FileStorageService;
import com.shivhub.backend.service.PurchaseService;

import lombok.RequiredArgsConstructor;


/*
 * =========================================================
 * PURCHASE CONTROLLER
 * =========================================================
 *
 * Seller Purchase APIs
 *
 * Features:
 *
 * 1. Create Purchase
 * 2. Upload Distributor Invoice
 * 3. Get Seller Purchases
 * 4. Get Purchase Count
 * 5. Get Purchase By ID
 * 6. Get Purchase Items
 * 7. Cancel Purchase
 *
 * Seller is identified from JWT.
 *
 * =========================================================
 */

@RestController
@RequestMapping("/api/purchases")
@RequiredArgsConstructor
@CrossOrigin(origins = "*")
public class PurchaseController {


    /*
     * =========================================================
     * DEPENDENCIES
     * =========================================================
     */

    private final PurchaseService purchaseService;

    private final UserRepository userRepository;

    private final PurchaseRepository purchaseRepository;

    private final FileStorageService fileStorageService;


    /*
     * =========================================================
     * GET AUTHENTICATED SELLER
     * =========================================================
     *
     * JWT
     *   ↓
     * Authentication
     *   ↓
     * Email
     *   ↓
     * UserRepository
     *   ↓
     * Seller
     *
     * =========================================================
     */

    private User getAuthenticatedSeller(
            Authentication authentication) {

        if (authentication == null) {

            throw new RuntimeException(
                    "Authentication required"
            );
        }


        String email =
                authentication.getName();


        if (email == null
                || email.isBlank()) {

            throw new RuntimeException(
                    "Authenticated seller email not found"
            );
        }


        return userRepository
                .findByEmail(email)
                .orElseThrow(
                        () -> new RuntimeException(
                                "Seller not found"
                        )
                );
    }


    /*
     * =========================================================
     * CREATE PURCHASE
     * =========================================================
     *
     * POST
     *
     * /api/purchases
     *
     * Content-Type:
     *
     * application/json
     *
     * =========================================================
     */

    @PostMapping
    public ResponseEntity<?> createPurchase(
            Authentication authentication,
            @RequestBody PurchaseRequest request) {

        try {

            User seller =
                    getAuthenticatedSeller(
                            authentication
                    );


            Purchase purchase =
                    purchaseService.createPurchase(
                            request,
                            seller
                    );


            return ResponseEntity
                    .status(HttpStatus.CREATED)
                    .body(purchase);


        } catch (RuntimeException exception) {

            return ResponseEntity
                    .badRequest()
                    .body(
                            Map.of(
                                    "message",
                                    getErrorMessage(exception)
                            )
                    );
        }
    }


    /*
     * =========================================================
     * UPLOAD DISTRIBUTOR ORIGINAL BILL
     * =========================================================
     *
     * POST
     *
     * /api/purchases/{purchaseId}/invoice
     *
     * Content-Type:
     *
     * multipart/form-data
     *
     * Form-data:
     *
     * file = invoice.pdf
     *
     * =========================================================
     */

    @PostMapping(
            value = "/{purchaseId}/invoice",
            consumes = "multipart/form-data"
    )
    public ResponseEntity<?> uploadPurchaseInvoice(
            Authentication authentication,
            @PathVariable Long purchaseId,
            @RequestPart("file") MultipartFile file) {

        try {

            /*
             * -------------------------------------------------
             * Authentication
             * -------------------------------------------------
             */

            User seller =
                    getAuthenticatedSeller(
                            authentication
                    );


            /*
             * -------------------------------------------------
             * Purchase ID validation
             * -------------------------------------------------
             */

            if (purchaseId == null) {

                return ResponseEntity
                        .badRequest()
                        .body(
                                Map.of(
                                        "message",
                                        "Purchase ID is required"
                                )
                        );
            }


            /*
             * -------------------------------------------------
             * File validation
             * -------------------------------------------------
             */

            if (file == null
                    || file.isEmpty()) {

                return ResponseEntity
                        .badRequest()
                        .body(
                                Map.of(
                                        "message",
                                        "Invoice file is required"
                                )
                        );
            }


            /*
             * -------------------------------------------------
             * Find purchase
             * -------------------------------------------------
             */

            Purchase purchase =
                    purchaseRepository
                            .findById(purchaseId)
                            .orElseThrow(
                                    () -> new RuntimeException(
                                            "Purchase not found: "
                                                    + purchaseId
                                    )
                            );


            /*
             * -------------------------------------------------
             * OWNERSHIP CHECK
             * -------------------------------------------------
             *
             * Logged-in seller:
             *
             * Seller 18
             *
             * Purchase seller:
             *
             * Seller 18
             *
             * Allowed.
             *
             * -------------------------------------------------
             */

            if (!isOwner(
                    purchase,
                    seller
            )) {

                return ResponseEntity
                        .status(HttpStatus.FORBIDDEN)
                        .body(
                                Map.of(
                                        "message",
                                        "You are not authorized to upload invoice for this purchase",

                                        "loggedInSellerId",
                                        seller.getId(),

                                        "purchaseSellerId",
                                        purchase.getSeller() == null
                                                ? null
                                                : purchase.getSeller().getId()
                                )
                        );
            }


            /*
             * -------------------------------------------------
             * STORE FILE
             * -------------------------------------------------
             */

            String invoicePath =
                    fileStorageService
                            .storePurchaseInvoice(
                                    purchaseId,
                                    file
                            );


            /*
             * -------------------------------------------------
             * UPDATE PURCHASE
             * -------------------------------------------------
             */

            purchase.setInvoiceFileUrl(
                    invoicePath
            );


            Purchase savedPurchase =
                    purchaseRepository.save(
                            purchase
                    );


            /*
             * -------------------------------------------------
             * SUCCESS
             * -------------------------------------------------
             */

            return ResponseEntity.ok(
                    Map.of(
                            "message",
                            "Distributor original bill uploaded successfully",

                            "purchaseId",
                            savedPurchase.getId(),

                            "invoiceFileUrl",
                            savedPurchase.getInvoiceFileUrl()
                    )
            );


        } catch (RuntimeException exception) {

            return ResponseEntity
                    .badRequest()
                    .body(
                            Map.of(
                                    "message",
                                    getErrorMessage(exception)
                            )
                    );
        }
    }


    /*
     * =========================================================
     * GET SELLER PURCHASE COUNT
     * =========================================================
     *
     * IMPORTANT:
     *
     * This endpoint is placed BEFORE:
     *
     * /{purchaseId}
     *
     * =========================================================
     *
     * GET
     *
     * /api/purchases/count
     *
     * =========================================================
     */

    @GetMapping("/count")
    public ResponseEntity<?> getPurchaseCount(
            Authentication authentication) {

        try {

            User seller =
                    getAuthenticatedSeller(
                            authentication
                    );


            long count =
                    purchaseService
                            .getSellerPurchaseCount(
                                    seller
                            );


            return ResponseEntity.ok(
                    Map.of(
                            "count",
                            count
                    )
            );


        } catch (RuntimeException exception) {

            return ResponseEntity
                    .badRequest()
                    .body(
                            Map.of(
                                    "message",
                                    getErrorMessage(exception)
                            )
                    );
        }
    }

    @GetMapping("/products/search")
    public ResponseEntity<?> searchPurchaseProducts(
            Authentication authentication,
            @RequestParam(defaultValue = "") String q,
            @RequestParam(required = false) Long categoryId,
            @RequestParam(required = false) Long subCategoryId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "12") int size) {
        try {
            return ResponseEntity.ok(purchaseService.searchSellerProducts(
                    getAuthenticatedSeller(authentication), q, categoryId, subCategoryId, page, size));
        } catch (RuntimeException exception) {
            return ResponseEntity.badRequest().body(Map.of("message", getErrorMessage(exception)));
        }
    }

    @GetMapping("/imeis/validate")
    public ResponseEntity<?> validateIncomingImei(
            Authentication authentication,
            @RequestParam Long productId,
            @RequestParam String code) {
        try {
            return ResponseEntity.ok(purchaseService.validateIncomingImei(
                    getAuthenticatedSeller(authentication), productId, code));
        } catch (RuntimeException exception) {
            return ResponseEntity.badRequest().body(Map.of("message", getErrorMessage(exception)));
        }
    }


    /*
     * =========================================================
     * GET SELLER PURCHASES
     * =========================================================
     *
     * GET
     *
     * /api/purchases
     *
     * =========================================================
     */

    @GetMapping
    public ResponseEntity<?> getSellerPurchases(
            Authentication authentication) {

        try {

            User seller =
                    getAuthenticatedSeller(
                            authentication
                    );


            List<Purchase> purchases =
                    purchaseService
                            .getSellerPurchases(
                                    seller
                            );


            return ResponseEntity.ok(
                    purchases
            );


        } catch (RuntimeException exception) {

            return ResponseEntity
                    .badRequest()
                    .body(
                            Map.of(
                                    "message",
                                    getErrorMessage(exception)
                            )
                    );
        }
    }


    /*
     * =========================================================
     * GET PURCHASE BY ID
     * =========================================================
     *
     * GET
     *
     * /api/purchases/4
     *
     * =========================================================
     */

    @GetMapping("/{purchaseId}")
    public ResponseEntity<?> getPurchaseById(
            Authentication authentication,
            @PathVariable Long purchaseId) {

        try {

            User seller =
                    getAuthenticatedSeller(
                            authentication
                    );


            Purchase purchase =
                    purchaseService
                            .getPurchaseById(
                                    purchaseId,
                                    seller
                            );


            return ResponseEntity.ok(
                    purchase
            );


        } catch (RuntimeException exception) {

            return ResponseEntity
                    .badRequest()
                    .body(
                            Map.of(
                                    "message",
                                    getErrorMessage(exception)
                            )
                    );
        }
    }


    /*
     * =========================================================
     * GET PURCHASE ITEMS
     * =========================================================
     *
     * GET
     *
     * /api/purchases/4/items
     *
     * =========================================================
     */

    @GetMapping("/{purchaseId}/items")
    public ResponseEntity<?> getPurchaseItems(
            Authentication authentication,
            @PathVariable Long purchaseId) {

        try {

            User seller =
                    getAuthenticatedSeller(
                            authentication
                    );


            List<PurchaseItem> items =
                    purchaseService
                            .getPurchaseItems(
                                    purchaseId,
                                    seller
                            );


            return ResponseEntity.ok(
                    items
            );


        } catch (RuntimeException exception) {

            return ResponseEntity
                    .badRequest()
                    .body(
                            Map.of(
                                    "message",
                                    getErrorMessage(exception)
                            )
                    );
        }
    }


    /*
     * =========================================================
     * CANCEL PURCHASE
     * =========================================================
     *
     * PUT
     *
     * /api/purchases/{purchaseId}/cancel
     *
     * Example:
     *
     * /api/purchases/4/cancel
     *
     * =========================================================
     */

    @PutMapping("/{purchaseId}/cancel")
    public ResponseEntity<?> cancelPurchase(
            Authentication authentication,
            @PathVariable Long purchaseId) {

        try {

            /*
             * -------------------------------------------------
             * Authentication
             * -------------------------------------------------
             */

            User seller =
                    getAuthenticatedSeller(
                            authentication
                    );


            /*
             * -------------------------------------------------
             * Cancel purchase
             * -------------------------------------------------
             */

            Purchase purchase =
                    purchaseService
                            .cancelPurchase(
                                    purchaseId,
                                    seller
                            );


            return ResponseEntity.ok(
                    purchase
            );


        } catch (RuntimeException exception) {

            return ResponseEntity
                    .badRequest()
                    .body(
                            Map.of(
                                    "message",
                                    getErrorMessage(exception)
                            )
                    );
        }
    }


    /*
     * =========================================================
     * CHECK PURCHASE OWNERSHIP
     * =========================================================
     */

    private boolean isOwner(
            Purchase purchase,
            User seller) {

        if (purchase == null
                || seller == null
                || seller.getId() == null) {

            return false;
        }


        if (purchase.getSeller() == null
                || purchase.getSeller().getId() == null) {

            return false;
        }


        return purchase
                .getSeller()
                .getId()
                .equals(
                        seller.getId()
                );
    }


    /*
     * =========================================================
     * ERROR MESSAGE HELPER
     * =========================================================
     */

    private String getErrorMessage(
            RuntimeException exception) {

        if (exception.getMessage() == null
                || exception.getMessage().isBlank()) {

            return "Request failed";
        }


        return exception.getMessage();
    }
}
