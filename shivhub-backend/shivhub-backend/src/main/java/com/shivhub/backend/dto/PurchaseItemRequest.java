package com.shivhub.backend.dto;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.LinkedHashMap;
import java.util.Map;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;


/*
 * =========================================================
 * PURCHASE ITEM REQUEST
 * =========================================================
 *
 * Represents one product being purchased from distributor.
 *
 * Supports:
 *
 * Product
 * Quantity
 * Unit
 * HSN Code
 * Purchase Price
 * Discount
 * GST
 * IMEI 1
 * IMEI 2
 * Serial Number
 *
 * =========================================================
 */

@Data
@NoArgsConstructor
@AllArgsConstructor
public class PurchaseItemRequest {


    /*
     * =========================================================
     * PRODUCT
     * =========================================================
     *
     * Existing ShivHub product ID.
     *
     * =========================================================
     */

    private Long productId;

    /** Optional exact variant selected for this stock receipt. */
    private Long variantId;


    /*
     * =========================================================
     * PRODUCT NAME
     * =========================================================
     *
     * Optional product name supplied from frontend.
     * Backend can also fetch the name from Product.
     *
     * =========================================================
     */

    private String productName;


    /*
     * =========================================================
     * SKU
     * =========================================================
     */

    private String sku;

    /** Optional supplier/EAN barcode snapshot; scanner/manual input is never treated as an IMEI. */
    private String barcode;


    /*
     * =========================================================
     * HSN CODE
     * =========================================================
     *
     * GST invoice HSN/SAC code.
     *
     * Example:
     *
     * 85171300
     *
     * =========================================================
     */

    private String hsnCode;

    /** Product colour recorded on the distributor invoice (for example, Blue). */
    private String color;

    /* Distributor-invoice product specification. These are preserved with the
     * purchase line so a later catalogue edit cannot lose the received details. */
    private String brand;
    private String model;
    private String ram;
    private String storage;
    private String description;

    /** Non-mobile subcategory specifications recorded as a purchase-line snapshot. */
    private Map<String, String> attributes = new LinkedHashMap<>();


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

    private String unit = "NOS";


    /*
     * =========================================================
     * QUANTITY
     * =========================================================
     *
     * Quantity received from distributor.
     *
     * =========================================================
     */

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

    private BigDecimal gstRate = BigDecimal.ZERO;

    /** Optional manual intra-state tax split. If present, CGST and SGST are
     * calculated separately instead of assuming an equal fixed GST rate. */
    private BigDecimal cgstRate;
    private BigDecimal sgstRate;


    /*
     * =========================================================
     * IMEI 1
     * =========================================================
     *
     * Mainly used for mobile phones.
     *
     * =========================================================
     */

    private String imei1;


    /*
     * =========================================================
     * IMEI 2
     * =========================================================
     *
     * Mainly used for dual-SIM mobile phones.
     *
     * =========================================================
     */

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

    private String serialNumber;

    /** True for mobiles: one IMEI record must be supplied per received unit. */
    private Boolean imeiTrackingRequired = false;

    /** One record per physical phone/electronic item when IMEI tracking is needed. */
    private List<PurchaseItemSerialRequest> serials = new ArrayList<>();
}
