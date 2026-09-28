package com.shivhub.backend.entity;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

import com.fasterxml.jackson.annotation.JsonBackReference;
import com.fasterxml.jackson.annotation.JsonManagedReference;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;


/*
 * =========================================================
 * PURCHASE ITEM ENTITY
 * =========================================================
 *
 * Represents one product inside a distributor purchase invoice.
 *
 * Example:
 *
 * Purchase Invoice
 *      ↓
 * ------------------------------------------------
 * OPPO A5       Qty 5
 * OPPO A6       Qty 3
 * OPPO Reno     Qty 2
 * ------------------------------------------------
 *
 * Each row above is a PurchaseItem.
 *
 *
 * GST INVOICE SUPPORT
 * =========================================================
 *
 * Stores:
 *
 * Product
 * Product Name
 * SKU
 * HSN Code
 * Unit
 * Quantity
 * Purchase Rate
 * Discount
 * Taxable Amount
 * GST Rate
 * CGST
 * SGST
 * IGST
 * Total
 *
 *
 * MOBILE / ELECTRONICS TRACKING
 * =========================================================
 *
 * Mobile:
 *      IMEI 1
 *      IMEI 2
 *
 * Other Electronics:
 *      Serial Number
 *
 * Examples:
 *
 * Mobile
 * TV
 * Laptop
 * Refrigerator
 * Washing Machine
 * Speaker
 * etc.
 *
 * =========================================================
 */

@Entity
@Table(name = "purchase_items")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class PurchaseItem {

    /* Product specification snapshot received on the distributor purchase bill. */
    @Column(name = "brand", length = 100)
    private String brand;

    @Column(name = "model", length = 150)
    private String model;

    @Column(name = "ram", length = 50)
    private String ram;

    @Column(name = "storage", length = 50)
    private String storage;

    @Column(name = "item_description", columnDefinition = "TEXT")
    private String description;

    /** JSON snapshot of the configured non-mobile specifications on receipt. */
    @Column(name = "attributes_json", columnDefinition = "TEXT")
    private String attributesJson;

    /** Barcode printed on the supplier invoice/package at the time stock is received. */
    @Column(name = "supplier_barcode", length = 150)
    private String supplierBarcode;


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
     * PURCHASE
     * =========================================================
     *
     * Many PurchaseItems belong to one Purchase.
     *
     * =========================================================
     */

    @JsonBackReference
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(
            name = "purchase_id",
            nullable = false
    )
    private Purchase purchase;


    /*
     * =========================================================
     * PRODUCT
     * =========================================================
     *
     * PurchaseItem points to the actual ShivHub Product.
     *
     * =========================================================
     */

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(
            name = "product_id",
            nullable = false
    )
    private Product product;

    /** Nullable for all historical/non-variant receipts. */
    @Column(name = "product_variant_id")
    private Long productVariantId;


    /*
     * =========================================================
     * PRODUCT NAME SNAPSHOT
     * =========================================================
     *
     * Product name at the time of purchase.
     *
     * Useful when product name changes later.
     *
     * =========================================================
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
     */

    @Column(name = "sku")
    private String sku;


    /*
     * =========================================================
     * HSN CODE
     * =========================================================
     *
     * Example:
     *
     * 85171300
     *
     * =========================================================
     */

    @Column(
            name = "hsn_code",
            length = 20
    )
    private String hsnCode;

    /* Colour is a purchase snapshot so invoice history remains accurate. */
    @Column(name = "color", length = 60)
    private String color;


    /*
     * =========================================================
     * UNIT
     * =========================================================
     *
     * Examples:
     *
     * NOS
     * PCS
     * BOX
     *
     * Default = NOS
     *
     * =========================================================
     */

    @Column(
            name = "unit",
            length = 20
    )
    private String unit = "NOS";


    /*
     * =========================================================
     * QUANTITY
     * =========================================================
     */

    @Column(nullable = false)
    private Integer quantity;


    /*
     * =========================================================
     * UNIT PRICE
     * =========================================================
     *
     * Distributor purchase price per unit.
     *
     * =========================================================
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
     * Item-level discount amount.
     *
     * =========================================================
     */

    @Column(
            precision = 12,
            scale = 2
    )
    private BigDecimal discount = BigDecimal.ZERO;


    /*
     * =========================================================
     * GST RATE
     * =========================================================
     *
     * Examples:
     *
     * 5
     * 12
     * 18
     * 28
     *
     * =========================================================
     */

    @Column(
            name = "gst_rate",
            precision = 5,
            scale = 2
    )
    private BigDecimal gstRate = BigDecimal.ZERO;


    /*
     * =========================================================
     * TAXABLE AMOUNT
     * =========================================================
     *
     * Quantity × Unit Price - Discount
     *
     * =========================================================
     */

    @Column(
            name = "taxable_amount",
            precision = 12,
            scale = 2
    )
    private BigDecimal taxableAmount = BigDecimal.ZERO;


    /*
     * =========================================================
     * CGST
     * =========================================================
     */

    @Column(
            precision = 12,
            scale = 2
    )
    private BigDecimal cgst = BigDecimal.ZERO;


    /*
     * =========================================================
     * SGST
     * =========================================================
     */

    @Column(
            precision = 12,
            scale = 2
    )
    private BigDecimal sgst = BigDecimal.ZERO;


    /*
     * =========================================================
     * IGST
     * =========================================================
     */

    @Column(
            precision = 12,
            scale = 2
    )
    private BigDecimal igst = BigDecimal.ZERO;


    /*
     * =========================================================
     * TOTAL PRICE
     * =========================================================
     *
     * Final line amount including GST.
     *
     * =========================================================
     */

    @Column(
            name = "total_price",
            precision = 12,
            scale = 2,
            nullable = false
    )
    private BigDecimal totalPrice = BigDecimal.ZERO;


    /*
     * =========================================================
     * IMEI 1
     * =========================================================
     *
     * Mainly used for mobile phones.
     *
     * =========================================================
     */

    @Column(
            name = "imei1",
            length = 30
    )
    private String imei1;


    /*
     * =========================================================
     * IMEI 2
     * =========================================================
     *
     * Used for dual-SIM mobile phones.
     *
     * =========================================================
     */

    @Column(
            name = "imei2",
            length = 30
    )
    private String imei2;


    /*
     * =========================================================
     * SERIAL NUMBER
     * =========================================================
     *
     * Used for:
     *
     * TV
     * Laptop
     * Refrigerator
     * Washing Machine
     * Speaker
     * Other Electronics
     *
     * =========================================================
     */

    @Column(
            name = "serial_number",
            length = 100
    )
    private String serialNumber;

    /** Retains whether this line was a mobile/IMEI-tracked inventory receipt. */
    @Column(name = "imei_tracking_required", nullable = false)
    private boolean imeiTrackingRequired = false;


    /*
     * =========================================================
     * IMEI / SERIAL RECORDS
     * =========================================================
     *
     * For future individual-unit tracking.
     *
     * Example:
     *
     * Purchase Item
     *      ↓
     *      ├── IMEI / Serial 1
     *      ├── IMEI / Serial 2
     *      └── IMEI / Serial 3
     *
     * =========================================================
     */

    @JsonManagedReference
    @OneToMany(
            mappedBy = "purchaseItem",
            cascade = CascadeType.ALL,
            orphanRemoval = true
    )
    private List<PurchaseItemSerial> serials = new ArrayList<>();


    /*
     * =========================================================
     * EXPLICIT IMEI 1 SETTER
     * =========================================================
     *
     * Kept explicitly so PurchaseService can safely call:
     *
     * purchaseItem.setImei1(...)
     *
     * =========================================================
     */

    public void setImei1(String imei1) {
        this.imei1 = imei1;
    }


    /*
     * =========================================================
     * EXPLICIT IMEI 2 SETTER
     * =========================================================
     */

    public void setImei2(String imei2) {
        this.imei2 = imei2;
    }


    /*
     * =========================================================
     * EXPLICIT SERIAL NUMBER SETTER
     * =========================================================
     */

    public void setSerialNumber(String serialNumber) {
        this.serialNumber = serialNumber;
    }


    /*
     * =========================================================
     * HELPER - ADD SERIAL
     * =========================================================
     *
     * Useful when individual serial records are created.
     *
     * =========================================================
     */

    public void addSerial(PurchaseItemSerial serial) {

        if (serial == null) {
            return;
        }

        serials.add(serial);
        serial.setPurchaseItem(this);
    }


    /*
     * =========================================================
     * HELPER - REMOVE SERIAL
     * =========================================================
     */

    public void removeSerial(PurchaseItemSerial serial) {

        if (serial == null) {
            return;
        }

        serials.remove(serial);
        serial.setPurchaseItem(null);
    }
}
