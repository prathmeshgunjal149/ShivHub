package com.shivhub.backend.dto;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;


/*
 * =========================================================
 * PURCHASE REQUEST
 * =========================================================
 *
 * Request used when Seller adds a purchase from distributor.
 *
 * Flow:
 *
 * Seller
 *    ↓
 * Distributor
 *    ↓
 * Invoice
 *    ↓
 * Products
 *
 * =========================================================
 */

@Data
@NoArgsConstructor
@AllArgsConstructor
public class PurchaseRequest {


    /*
     * =========================================================
     * SELLER-DISTRIBUTOR RELATIONSHIP
     * =========================================================
     *
     * SellerDistributor ID.
     *
     * Seller
     *    ↓
     * SellerDistributor
     *    ↓
     * Distributor
     *
     * =========================================================
     */

    private Long sellerDistributorId;


    /*
     * =========================================================
     * DISTRIBUTOR INVOICE NUMBER
     * =========================================================
     */

    private String invoiceNumber;

    /* GST/e-invoice header values printed by the distributor. */
    private String irn;
    private String acknowledgementNumber;
    private LocalDateTime acknowledgementDate;
    private String ewayBillNumber;
    private String modeTermsOfPayment;
    private String deliveryNote;
    private String referenceNumber;
    private String otherReferences;
    private String buyerOrderNumber;
    private String dispatchDocumentNumber;
    private LocalDateTime deliveryNoteDate;
    private String dispatchedThrough;
    private String destination;
    private String termsOfDelivery;
    private String eInvoiceQrImageUrl;
    private BigDecimal roundOff = BigDecimal.ZERO;


    /*
     * =========================================================
     * PURCHASE DATE
     * =========================================================
     */

    private LocalDateTime purchaseDate;


    /*
     * =========================================================
     * DISCOUNT
     * =========================================================
     *
     * Overall purchase discount.
     *
     * =========================================================
     */

    private BigDecimal discount = BigDecimal.ZERO;


    /*
     * =========================================================
     * NOTES
     * =========================================================
     */

    private String notes;


    /*
     * =========================================================
     * INVOICE FILE URL
     * =========================================================
     *
     * Example:
     *
     * uploads/purchases/1/invoice.pdf
     *
     * =========================================================
     */

    private String invoiceFileUrl;


    /*
     * =========================================================
     * PURCHASE ITEMS
     * =========================================================
     *
     * One purchase can contain multiple products.
     *
     * =========================================================
     */

    private List<PurchaseItemRequest> items = new ArrayList<>();
}
