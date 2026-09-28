package com.shivhub.backend.dto;

import java.math.BigDecimal;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;


/*
 * =========================================================
 * PURCHASE PAYMENT SUMMARY RESPONSE
 * =========================================================
 *
 * Shows:
 *
 * Purchase Total
 * Total Paid
 * Remaining Due
 * Payment Status
 *
 * =========================================================
 */

@Data
@NoArgsConstructor
@AllArgsConstructor
public class PurchasePaymentSummaryResponse {

    /*
     * Original purchase invoice total.
     */
    private BigDecimal purchaseTotal;


    /*
     * Total amount paid to distributor.
     */
    private BigDecimal totalPaid;


    /*
     * Remaining amount to be paid.
     */
    private BigDecimal remainingAmount;


    /*
     * UNPAID
     * PARTIALLY_PAID
     * PAID
     */
    private String paymentStatus;
}