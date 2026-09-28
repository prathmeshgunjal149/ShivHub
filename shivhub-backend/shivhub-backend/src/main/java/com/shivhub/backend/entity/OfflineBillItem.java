package com.shivhub.backend.entity;

import java.math.BigDecimal;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;


/*
 * =========================================================
 * OfflineBillItem
 * =========================================================
 *
 * Represents one product inside an Offline Bill.
 *
 * Example:
 *
 * Bill #SH-BILL-20260901-001
 *
 *   iPhone 15        Qty: 1
 *   Boat Earbuds     Qty: 2
 *   Mobile Cover     Qty: 1
 *
 * Each product is stored as one OfflineBillItem.
 *
 * =========================================================
 */

@Entity
@Table(name = "offline_bill_items")

@Data
@NoArgsConstructor
@AllArgsConstructor

public class OfflineBillItem {
    @Column(name = "product_variant_id") private Long productVariantId;
    @Column(name = "selected_attributes", columnDefinition = "TEXT") private String selectedAttributes;


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
     * OFFLINE BILL
     * =========================================================
     *
     * Many items belong to one offline bill.
     *
     */

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(
            name = "offline_bill_id",
            nullable = false
    )
    private OfflineBill offlineBill;


    /*
     * =========================================================
     * PRODUCT ID
     * =========================================================
     *
     * Stores the product ID.
     *
     */

    @Column(
            name = "product_id",
            nullable = false
    )
    private Long productId;


    /*
     * =========================================================
     * PRODUCT NAME SNAPSHOT
     * =========================================================
     *
     * Product name at the time of billing.
     *
     * If admin changes product name later,
     * old bill should still show the original name.
     *
     */

    @Column(
            name = "product_name",
            nullable = false
    )
    private String productName;


    /*
     * =========================================================
     * SKU
     * =========================================================
     *
     * Product SKU / code.
     *
     * Optional.
     *
     */

    @Column(
            name = "sku"
    )
    private String sku;

    @Column(name = "mrp", precision = 12, scale = 2)
    private BigDecimal mrp;


    /*
     * =========================================================
     * QUANTITY
     * =========================================================
     */

    @Column(
            name = "quantity",
            nullable = false
    )
    private Integer quantity;


    /*
     * =========================================================
     * UNIT PRICE
     * =========================================================
     *
     * Actual selling price per unit at billing time.
     *
     */

    @Column(
            name = "unit_price",
            precision = 12,
            scale = 2,
            nullable = false
    )
    private BigDecimal unitPrice;


    /*
     * =========================================================
     * DISCOUNT
     * =========================================================
     *
     * Discount applied to this particular item.
     *
     */

    @Column(
            name = "discount",
            precision = 12,
            scale = 2,
            nullable = false
    )
    private BigDecimal discount = BigDecimal.ZERO;


    /*
     * =========================================================
     * TAXABLE AMOUNT
     * =========================================================
     *
     * Amount after item discount and before GST.
     *
     */

    @Column(
            name = "taxable_amount",
            precision = 12,
            scale = 2,
            nullable = false
    )
    private BigDecimal taxableAmount = BigDecimal.ZERO;


    /*
     * =========================================================
     * GST RATE
     * =========================================================
     *
     * Example:
     *
     * 5
     * 12
     * 18
     * 28
     *
     */

    @Column(
            name = "gst_rate",
            precision = 5,
            scale = 2,
            nullable = false
    )
    private BigDecimal gstRate = BigDecimal.ZERO;


    /*
     * =========================================================
     * CGST
     * =========================================================
     */

    @Column(
            name = "cgst",
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
            name = "sgst",
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
            name = "igst",
            precision = 12,
            scale = 2,
            nullable = false
    )
    private BigDecimal igst = BigDecimal.ZERO;


    /*
     * =========================================================
     * TOTAL PRICE
     * =========================================================
     *
     * Final amount for this item including applicable tax.
     *
     * Formula:
     *
     * taxableAmount
     * + CGST
     * + SGST
     * + IGST
     *
     */

    @Column(
            name = "total_price",
            precision = 12,
            scale = 2,
            nullable = false
    )
    private BigDecimal totalPrice = BigDecimal.ZERO;
}
