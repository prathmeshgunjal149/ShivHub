package com.shivhub.backend.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.shivhub.backend.dto.ImeiHistoryResponse;
import com.shivhub.backend.entity.Distributor;
import com.shivhub.backend.entity.OfflineBill;
import com.shivhub.backend.entity.OfflineBillItem;
import com.shivhub.backend.entity.Product;
import com.shivhub.backend.entity.Purchase;
import com.shivhub.backend.entity.PurchaseItem;
import com.shivhub.backend.entity.PurchaseItemSerial;
import com.shivhub.backend.entity.SellerDistributor;
import com.shivhub.backend.service.OfflineBillImeiService;

import lombok.RequiredArgsConstructor;


/*
 * =========================================================
 * OFFLINE BILL IMEI CONTROLLER
 * =========================================================
 *
 * APIs:
 *
 * 1. GET AVAILABLE IMEI
 *
 * GET:
 * /api/offline-bills/imei/available?productId=123
 *
 *
 * 2. GET IMEI HISTORY
 *
 * GET:
 * /api/offline-bills/imei/history?imei=XXXXXXXXXXXXXXX
 *
 *
 * FLOW:
 *
 * Purchase
 *      ↓
 * PurchaseItem
 *      ↓
 * PurchaseItemSerial
 *      ↓
 * IMEI / Serial
 *      ↓
 * OfflineBillItem
 *      ↓
 * OfflineBill
 *      ↓
 * Customer
 *
 * =========================================================
 */

@RestController
@RequestMapping("/api/offline-bills/imei")
@RequiredArgsConstructor
public class OfflineBillImeiController {


    /*
     * =========================================================
     * SERVICE
     * =========================================================
     */

    private final OfflineBillImeiService imeiService;


    /*
     * =========================================================
     * GET AVAILABLE IMEI
     * =========================================================
     *
     * Used by Seller POS.
     *
     * Returns available IMEI records for:
     *
     * 1. Logged-in seller
     * 2. Selected product
     * 3. Completed purchase
     * 4. Unsold physical units
     *
     *
     * Example:
     *
     * GET
     * /api/offline-bills/imei/available?productId=123
     *
     * =========================================================
     */

    @GetMapping("/available")
    @PreAuthorize("hasRole('SELLER')")
    public ResponseEntity<?> available(
            @RequestParam("productId") Long productId,
            Authentication authentication) {


        /*
         * =====================================================
         * AUTHENTICATION CHECK
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
         * GET AVAILABLE IMEI
         * =====================================================
         */

        return ResponseEntity.ok(
                imeiService.available(
                        sellerEmail,
                        productId
                )
        );
    }


    /*
     * =========================================================
     * IMEI HISTORY
     * =========================================================
     *
     * GET:
     *
     * /api/offline-bills/imei/history?imei=XXXXXXXXXXXXXXX
     *
     *
     * SELLER / ADMIN
     *
     * =========================================================
     */

    @GetMapping("/history")
    @PreAuthorize("hasAnyRole('SELLER', 'ADMIN')")
    public ResponseEntity<ImeiHistoryResponse> history(
            @RequestParam("imei") String imei) {


        /*
         * =====================================================
         * BASIC VALIDATION
         * =====================================================
         */

        if (imei == null || imei.isBlank()) {

            return ResponseEntity
                    .badRequest()
                    .build();
        }


        /*
         * =====================================================
         * GET IMEI RECORD
         * =====================================================
         */

        PurchaseItemSerial serial =
                imeiService.getImeiHistory(
                        imei.trim()
                );


        /*
         * =====================================================
         * ENTITY -> DTO
         * =====================================================
         */

        ImeiHistoryResponse response =
                mapToResponse(serial);


        return ResponseEntity.ok(response);
    }


    /*
     * =========================================================
     * ENTITY -> RESPONSE
     * =========================================================
     */

    private ImeiHistoryResponse mapToResponse(
            PurchaseItemSerial serial) {


        ImeiHistoryResponse response =
                new ImeiHistoryResponse();


        /*
         * =====================================================
         * IMEI / SERIAL INFORMATION
         * =====================================================
         */

        response.setSerialId(
                serial.getId()
        );

        response.setImei1(
                serial.getImei1()
        );

        response.setImei2(
                serial.getImei2()
        );

        response.setSerialNumber(
                serial.getSerialNumber()
        );


        /*
         * =====================================================
         * PURCHASE ITEM
         * =====================================================
         */

        PurchaseItem item =
                serial.getPurchaseItem();


        if (item == null) {

            response.setSaleStatus(
                    "UNKNOWN"
            );

            return response;
        }


        /*
         * =====================================================
         * PRODUCT
         * =====================================================
         */

        Product product =
                item.getProduct();


        if (product != null) {

            response.setProductId(
                    product.getId()
            );

            response.setProductName(
                    product.getName()
            );

            response.setBrand(
                    product.getBrand()
            );
        }


        /*
         * =====================================================
         * PURCHASE
         * =====================================================
         */

        Purchase purchase =
                item.getPurchase();


        if (purchase != null) {

            response.setPurchaseId(
                    purchase.getId()
            );

            response.setPurchaseInvoiceNumber(
                    purchase.getInvoiceNumber()
            );

            response.setPurchaseDate(
                    purchase.getPurchaseDate()
            );


            /*
             * =================================================
             * DISTRIBUTOR
             * =================================================
             *
             * Purchase
             *      ↓
             * SellerDistributor
             *      ↓
             * Distributor
             *
             * =================================================
             */

            SellerDistributor sellerDistributor =
                    purchase.getDistributor();


            if (sellerDistributor != null) {

                Distributor distributor =
                        sellerDistributor.getDistributor();


                if (distributor != null) {

                    response.setDistributorId(
                            distributor.getId()
                    );

                    response.setDistributorName(
                            distributor.getBusinessName()
                    );
                }
            }
        }


        /*
         * =====================================================
         * SOLD OFFLINE BILL ITEM
         * =====================================================
         */

        OfflineBillItem soldItem =
                serial.getSoldOfflineBillItem();


        /*
         * =====================================================
         * AVAILABLE
         * =====================================================
         *
         * If no OfflineBillItem is attached,
         * the physical unit is currently available.
         *
         * =====================================================
         */

        if (soldItem == null) {

            response.setSaleStatus(
                    "AVAILABLE"
            );

            return response;
        }


        /*
         * =====================================================
         * SOLD
         * =====================================================
         */

        response.setSaleStatus(
                "SOLD"
        );


        /*
         * =====================================================
         * SALE ITEM DETAILS
         * =====================================================
         */

        response.setSellingPrice(
                soldItem.getUnitPrice()
        );

        response.setTaxableAmount(
                soldItem.getTaxableAmount()
        );

        response.setCgst(
                soldItem.getCgst()
        );

        response.setSgst(
                soldItem.getSgst()
        );

        response.setIgst(
                soldItem.getIgst()
        );

        response.setTotalPrice(
                soldItem.getTotalPrice()
        );


        /*
         * =====================================================
         * OFFLINE BILL
         * =====================================================
         */

        OfflineBill bill =
                soldItem.getOfflineBill();


        if (bill != null) {

            response.setSaleBillId(
                    bill.getId()
            );

            response.setSaleBillNumber(
                    bill.getBillNumber()
            );

            response.setSaleDate(
                    bill.getCreatedAt()
            );


            /*
             * =================================================
             * CUSTOMER
             * =================================================
             */

            response.setCustomerId(
                    bill.getCustomerId()
            );

            response.setCustomerName(
                    bill.getCustomerName()
            );

            response.setCustomerMobile(
                    bill.getCustomerMobile()
            );

            response.setCustomerEmail(
                    bill.getCustomerEmail()
            );
        }


        /*
         * =====================================================
         * FINAL RESPONSE
         * =====================================================
         */

        return response;
    }
}