package com.shivhub.backend.dto;

import java.math.BigDecimal;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;


/*
 * =========================================================
 * OfflineBillItemResponse
 * =========================================================
 *
 * One product returned inside an Offline Bill response.
 *
 * =========================================================
 */

@Data
@NoArgsConstructor
@AllArgsConstructor
public class OfflineBillItemResponse {


    /*
     * Item ID
     */

    private Long itemId;


    /*
     * Product ID
     */

    private Long productId;


    /*
     * Product name snapshot
     */

    private String productName;


    /*
     * SKU
     */

    private String sku;


    /*
     * Quantity
     */

    private Integer quantity;


    /*
     * Unit selling price
     */

    private BigDecimal unitPrice;


    /*
     * Item discount
     */

    private BigDecimal discount;


    /*
     * Taxable amount
     */

    private BigDecimal taxableAmount;


    /*
     * GST rate
     */

    private BigDecimal gstRate;


    /*
     * CGST
     */

    private BigDecimal cgst;


    /*
     * SGST
     */

    private BigDecimal sgst;


    /*
     * IGST
     */

    private BigDecimal igst;


    /*
     * Final item total
     */

    private BigDecimal totalPrice;
}