package com.shivhub.backend.dto;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import com.shivhub.backend.enums.PaymentMethod;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;


/*
 * =========================================================
 * PURCHASE PAYMENT REQUEST
 * =========================================================
 *
 * Used when Seller records a payment made to Distributor.
 *
 * =========================================================
 */

@Data
@NoArgsConstructor
@AllArgsConstructor
public class PurchasePaymentRequest {

    /*
     * Purchase for which payment is being made.
     */
    private Long purchaseId;


    /*
     * Amount paid.
     */
    private BigDecimal amount;


    /*
     * Payment method.
     */
    private PaymentMethod paymentMethod;


    /*
     * Date and time of payment.
     */
    private LocalDateTime paymentDate;


    /*
     * UPI Transaction ID /
     * Bank Reference /
     * Cheque Number.
     */
    private String transactionReference;


    /*
     * Optional notes.
     */
    private String notes;
}