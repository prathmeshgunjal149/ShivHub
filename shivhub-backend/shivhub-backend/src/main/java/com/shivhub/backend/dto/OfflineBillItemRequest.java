package com.shivhub.backend.dto;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;


/*
 * =========================================================
 * OfflineBillItemRequest
 * =========================================================
 *
 * One product coming from Seller POS billing screen.
 *
 * Handles:
 *
 * 1. Product
 * 2. Quantity
 * 3. Item discount
 * 4. GST rate
 * 5. Selected IMEI / serial IDs
 *
 * Example:
 *
 * {
 *   "productId": 10,
 *   "quantity": 1,
 *   "discount": 500,
 *   "gstRate": 18,
 *   "purchaseSerialIds": [25]
 * }
 *
 * For a non-IMEI product:
 *
 * "purchaseSerialIds": []
 *
 * For a mobile:
 *
 * "purchaseSerialIds": [25, 26]
 *
 * =========================================================
 */

public class OfflineBillItemRequest {
    private Long variantId;
    public Long getVariantId() { return variantId; }
    public void setVariantId(Long variantId) { this.variantId = variantId; }

    /*
     * =========================================================
     * PRODUCT ID
     * =========================================================
     */

    @NotNull(message = "Product ID is required")
    private Long productId;


    /*
     * =========================================================
     * QUANTITY
     * =========================================================
     */

    @NotNull(message = "Quantity is required")
    @Min(
            value = 1,
            message = "Quantity must be at least 1"
    )
    private Integer quantity;


    /*
     * Optional seller-entered POS unit price. When omitted, the current
     * approved product selling price remains the source, preserving old APIs.
     */
    private BigDecimal unitPrice;


    /*
     * =========================================================
     * ITEM DISCOUNT
     * =========================================================
     *
     * Discount applicable only to this item.
     *
     * Example:
     *
     * Product amount = 20,000
     * Item discount  = 1,000
     *
     * Taxable amount = 19,000
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
     * 0
     * 5
     * 12
     * 18
     * 28
     *
     * =========================================================
     */

    private BigDecimal gstRate = BigDecimal.ZERO;


    /*
     * =========================================================
     * PURCHASE SERIAL / IMEI IDS
     * =========================================================
     *
     * These are database IDs of PurchaseItemSerial.
     *
     * IMPORTANT:
     *
     * Browser should NOT send IMEI numbers directly for sale.
     *
     * Frontend first gets available IMEI units:
     *
     * GET /api/offline-bills/imei/available
     *
     * Then sends the selected database IDs.
     *
     * Backend verifies:
     *
     * - Seller owns the stock
     * - Product matches
     * - Purchase is COMPLETED
     * - IMEI is not already sold
     * - Quantity matches selected IMEIs
     *
     * Non-IMEI product:
     *
     * []
     *
     * One mobile:
     *
     * [25]
     *
     * Two mobiles:
     *
     * [25, 26]
     *
     * =========================================================
     */

    private List<Long> purchaseSerialIds =
            new ArrayList<>();


    /*
     * =========================================================
     * DEFAULT CONSTRUCTOR
     * =========================================================
     */

    public OfflineBillItemRequest() {
    }


    /*
     * =========================================================
     * GET PRODUCT ID
     * =========================================================
     */

    public Long getProductId() {
        return productId;
    }


    /*
     * =========================================================
     * SET PRODUCT ID
     * =========================================================
     */

    public void setProductId(Long productId) {
        this.productId = productId;
    }


    /*
     * =========================================================
     * GET QUANTITY
     * =========================================================
     */

    public Integer getQuantity() {
        return quantity;
    }


    /*
     * =========================================================
     * SET QUANTITY
     * =========================================================
     */

    public void setQuantity(Integer quantity) {
        this.quantity = quantity;
    }


    public BigDecimal getUnitPrice() {
        return unitPrice;
    }


    public void setUnitPrice(BigDecimal unitPrice) {
        this.unitPrice = unitPrice;
    }


    /*
     * =========================================================
     * GET DISCOUNT
     * =========================================================
     */

    public BigDecimal getDiscount() {
        return discount;
    }


    /*
     * =========================================================
     * SET DISCOUNT
     * =========================================================
     */

    public void setDiscount(BigDecimal discount) {

        this.discount =
                discount == null
                        ? BigDecimal.ZERO
                        : discount;
    }


    /*
     * =========================================================
     * GET GST RATE
     * =========================================================
     */

    public BigDecimal getGstRate() {
        return gstRate;
    }


    /*
     * =========================================================
     * SET GST RATE
     * =========================================================
     */

    public void setGstRate(BigDecimal gstRate) {

        this.gstRate =
                gstRate == null
                        ? BigDecimal.ZERO
                        : gstRate;
    }


    /*
     * =========================================================
     * GET PURCHASE SERIAL IDS
     * =========================================================
     */

    public List<Long> getPurchaseSerialIds() {

        return purchaseSerialIds;
    }


    /*
     * =========================================================
     * SET PURCHASE SERIAL IDS
     * =========================================================
     */

    public void setPurchaseSerialIds(
            List<Long> purchaseSerialIds) {

        this.purchaseSerialIds =
                purchaseSerialIds == null
                        ? new ArrayList<>()
                        : new ArrayList<>(purchaseSerialIds);
    }
}
