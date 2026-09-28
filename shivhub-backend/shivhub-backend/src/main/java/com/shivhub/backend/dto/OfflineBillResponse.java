package com.shivhub.backend.dto;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

import com.shivhub.backend.enums.PaymentMethod;
import com.shivhub.backend.enums.PaymentStatus;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;


/*
 * =========================================================
 * OfflineBillResponse
 * =========================================================
 *
 * Response returned after:
 *
 * 1. Bill creation
 * 2. Bill details
 * 3. Seller billing history
 * 4. Admin billing history
 * 5. Reports
 *
 * =========================================================
 */

@Data
@NoArgsConstructor
@AllArgsConstructor
public class OfflineBillResponse {


    /*
     * =========================================================
     * BILL INFORMATION
     * =========================================================
     */

    private Long billId;

    private String billNumber;


    /*
     * =========================================================
     * SELLER INFORMATION
     * =========================================================
     */

    private Long sellerId;

    private String sellerName;

    private Long salesPersonId;

    private String salesPersonName;


    /*
     * =========================================================
     * CUSTOMER INFORMATION
     * =========================================================
     */

    private Long customerId;

    private String customerName;

    private String customerMobile;

    private String customerEmail;

    private String customerGstin;


    /*
     * =========================================================
     * BILL ITEMS
     * =========================================================
     */

    private List<OfflineBillItemResponse> items;


    /*
     * =========================================================
     * AMOUNTS
     * =========================================================
     */

    private BigDecimal subtotal;

    private BigDecimal discount;

    private BigDecimal taxableAmount;

    private BigDecimal cgst;

    private BigDecimal sgst;

    private BigDecimal igst;

    private BigDecimal deliveryCharge;

    private BigDecimal grandTotal;


    /*
     * =========================================================
     * PAYMENT
     * =========================================================
     */

    private PaymentMethod paymentMethod;

    private PaymentStatus paymentStatus;

    private BigDecimal paymentAmount;

    private String transactionId;


    /*
     * =========================================================
     * OTHER
     * =========================================================
     */

    private String notes;

    private LocalDateTime createdAt;

    private LocalDateTime updatedAt;
}
