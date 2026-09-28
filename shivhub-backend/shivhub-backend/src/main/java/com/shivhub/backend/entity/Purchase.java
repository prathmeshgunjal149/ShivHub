package com.shivhub.backend.entity;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

import com.shivhub.backend.enums.PurchaseStatus;
import com.fasterxml.jackson.annotation.JsonManagedReference;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;


/*
 * =========================================================
 * PURCHASE ENTITY
 * =========================================================
 *
 * Distributor → Seller Purchase
 *
 * Example:
 *
 * OPPO Distributor
 *       ↓
 * Purchase Invoice
 *       ↓
 * ShivHub Seller
 *
 *
 * One Purchase can contain multiple PurchaseItems.
 *
 * Example:
 *
 * Purchase #1
 *      ├── OPPO A5
 *      ├── OPPO A6
 *      └── OPPO Reno
 *
 * =========================================================
 */

@Entity
@Table(name = "purchases")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class Purchase {


    /*
     * =========================================================
     * PRIMARY KEY
     * =========================================================
     */

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;


    /*
     * =========================================================
     * PURCHASE STATUS
     * =========================================================
     *
     * COMPLETED
     * CANCELLED
     *
     * Purchase delete/cancel feature will use this.
     *
     * We keep the purchase record in database and change
     * status instead of physically deleting it.
     *
     * =========================================================
     */

    @Enumerated(EnumType.STRING)
    @Column(
            nullable = false,
            length = 20
    )
    private PurchaseStatus status = PurchaseStatus.COMPLETED;


    /*
     * =========================================================
     * PURCHASE / INVOICE NUMBER
     * =========================================================
     *
     * This is the invoice number given by distributor.
     *
     * Example:
     *
     * OPPO-INV-2026-00125
     *
     * =========================================================
     */

    @Column(
            name = "invoice_number",
            nullable = false
    )
    private String invoiceNumber;

    /* E-invoice / transport details are stored for GST audit and purchase history. */
    @Column(length = 100)
    private String irn;

    @Column(name = "acknowledgement_number", length = 100)
    private String acknowledgementNumber;

    @Column(name = "acknowledgement_date")
    private LocalDateTime acknowledgementDate;

    @Column(name = "eway_bill_number", length = 100)
    private String ewayBillNumber;

    @Column(name = "mode_terms_of_payment", length = 150)
    private String modeTermsOfPayment;

    @Column(name = "delivery_note", length = 100)
    private String deliveryNote;

    @Column(name = "reference_number", length = 100)
    private String referenceNumber;

    @Column(name = "other_references", length = 255)
    private String otherReferences;

    @Column(name = "buyer_order_number", length = 100)
    private String buyerOrderNumber;

    @Column(name = "dispatch_document_number", length = 100)
    private String dispatchDocumentNumber;

    @Column(name = "delivery_note_date")
    private LocalDateTime deliveryNoteDate;

    @Column(name = "dispatched_through", length = 150)
    private String dispatchedThrough;

    @Column(length = 150)
    private String destination;

    @Column(name = "terms_of_delivery", columnDefinition = "TEXT")
    private String termsOfDelivery;

    @Column(name = "e_invoice_qr_image_url", length = 500)
    private String eInvoiceQrImageUrl;

    @Column(name = "round_off", precision = 12, scale = 2)
    private BigDecimal roundOff = BigDecimal.ZERO;


    /*
     * =========================================================
     * DISTRIBUTOR
     * =========================================================
     *
     * Purchase is connected with SellerDistributor.
     *
     * Seller
     *    ↓
     * SellerDistributor
     *    ↓
     * Distributor
     *
     * =========================================================
     */

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(
            name = "distributor_id",
            nullable = false
    )
    private SellerDistributor distributor;


    /*
     * =========================================================
     * SELLER
     * =========================================================
     *
     * Purchase belongs to the seller/shop.
     *
     * =========================================================
     */

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(
            name = "seller_id",
            nullable = false
    )
    private User seller;


    /*
     * =========================================================
     * PURCHASE DATE
     * =========================================================
     */

    @Column(
            name = "purchase_date",
            nullable = false
    )
    private LocalDateTime purchaseDate;


    /*
     * =========================================================
     * SUBTOTAL
     * =========================================================
     *
     * Product value before GST.
     *
     * =========================================================
     */

    @Column(
            precision = 12,
            scale = 2,
            nullable = false
    )
    private BigDecimal subtotal = BigDecimal.ZERO;


    /*
     * =========================================================
     * CGST
     * =========================================================
     */

    @Column(
            precision = 12,
            scale = 2,
            nullable = false
    )
    private BigDecimal cgst = BigDecimal.ZERO;


    /*
     * =========================================================
     * SGST
     * =========================================================
     */

    @Column(
            precision = 12,
            scale = 2,
            nullable = false
    )
    private BigDecimal sgst = BigDecimal.ZERO;


    /*
     * =========================================================
     * IGST
     * =========================================================
     */

    @Column(
            precision = 12,
            scale = 2,
            nullable = false
    )
    private BigDecimal igst = BigDecimal.ZERO;


    /*
     * =========================================================
     * DISCOUNT
     * =========================================================
     */

    @Column(
            precision = 12,
            scale = 2,
            nullable = false
    )
    private BigDecimal discount = BigDecimal.ZERO;


    /*
     * =========================================================
     * GRAND TOTAL
     * =========================================================
     *
     * Final amount of distributor invoice.
     *
     * Example:
     *
     * Subtotal = 500000
     * CGST     = 45000
     * SGST     = 45000
     *
     * Grand Total = 590000
     *
     * =========================================================
     */

    @Column(
            name = "grand_total",
            precision = 12,
            scale = 2,
            nullable = false
    )
    private BigDecimal grandTotal = BigDecimal.ZERO;


    /*
     * =========================================================
     * DISTRIBUTOR INVOICE FILE
     * =========================================================
     *
     * Example:
     *
     * uploads/purchases/1/invoice.pdf
     *
     * =========================================================
     */

    @Column(name = "invoice_file_url")
    private String invoiceFileUrl;


    /*
     * =========================================================
     * NOTES
     * =========================================================
     */

    @Column(columnDefinition = "TEXT")
    private String notes;


    /*
     * =========================================================
     * PURCHASE ITEMS
     * =========================================================
     *
     * One Purchase has many PurchaseItems.
     *
     * Purchase
     *    ↓
     * PurchaseItem
     *    ↓
     * Product
     *
     * =========================================================
     */

    @JsonManagedReference
    @OneToMany(
            mappedBy = "purchase",
            cascade = CascadeType.ALL,
            orphanRemoval = true
    )
    private List<PurchaseItem> items = new ArrayList<>();


    /*
     * =========================================================
     * CREATED AT
     * =========================================================
     */

    @Column(
            name = "created_at",
            nullable = false,
            updatable = false
    )
    private LocalDateTime createdAt;


    /*
     * =========================================================
     * UPDATED AT
     * =========================================================
     */

    @Column(
            name = "updated_at",
            nullable = false
    )
    private LocalDateTime updatedAt;


    /*
     * =========================================================
     * PRE-PERSIST
     * =========================================================
     */

    @PrePersist
    protected void onCreate() {

        LocalDateTime now = LocalDateTime.now();

        /*
         * Default purchase date.
         */

        if (purchaseDate == null) {
            purchaseDate = now;
        }

        /*
         * Default status.
         */

        if (status == null) {
            status = PurchaseStatus.COMPLETED;
        }

        /*
         * Default financial values.
         */

        if (subtotal == null) {
            subtotal = BigDecimal.ZERO;
        }

        if (cgst == null) {
            cgst = BigDecimal.ZERO;
        }

        if (sgst == null) {
            sgst = BigDecimal.ZERO;
        }

        if (igst == null) {
            igst = BigDecimal.ZERO;
        }

        if (discount == null) {
            discount = BigDecimal.ZERO;
        }

        if (grandTotal == null) {
            grandTotal = BigDecimal.ZERO;
        }

        /*
         * Timestamps.
         */

        createdAt = now;
        updatedAt = now;
    }


    /*
     * =========================================================
     * PRE-UPDATE
     * =========================================================
     */

    @PreUpdate
    protected void onUpdate() {

        updatedAt = LocalDateTime.now();
    }
}
