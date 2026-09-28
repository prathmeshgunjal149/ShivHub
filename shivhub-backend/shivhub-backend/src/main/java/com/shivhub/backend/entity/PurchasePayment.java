package com.shivhub.backend.entity;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import com.shivhub.backend.enums.PaymentMethod;

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
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;


/*
 * =========================================================
 * PURCHASE PAYMENT ENTITY
 * =========================================================
 *
 * Stores payments made by Seller to Distributor.
 *
 * One Purchase
 *      ↓
 * Multiple Payments
 *
 * Example:
 *
 * Purchase Total = ₹2,07,609
 *
 * Payment 1 = ₹50,000 UPI
 * Payment 2 = ₹50,000 Cash
 *
 * Total Paid = ₹1,00,000
 * Balance    = ₹1,07,609
 *
 * =========================================================
 */

@Entity
@Table(name = "purchase_payments")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class PurchasePayment {


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
     * Many payments can belong to one purchase.
     *
     * =========================================================
     */

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(
            name = "purchase_id",
            nullable = false
    )
    private Purchase purchase;


    /*
     * =========================================================
     * PAYMENT AMOUNT
     * =========================================================
     */

    @Column(
            name = "amount",
            precision = 12,
            scale = 2,
            nullable = false
    )
    private BigDecimal amount;


    /*
     * =========================================================
     * PAYMENT METHOD
     * =========================================================
     *
     * CASH
     * UPI
     * BANK_TRANSFER
     * CHEQUE
     * OTHER
     *
     * =========================================================
     */

    @Enumerated(EnumType.STRING)
    @Column(
            name = "payment_method",
            nullable = false,
            length = 30
    )
    private PaymentMethod paymentMethod;


    /*
     * =========================================================
     * PAYMENT DATE
     * =========================================================
     */

    @Column(
            name = "payment_date",
            nullable = false
    )
    private LocalDateTime paymentDate;


    /*
     * =========================================================
     * TRANSACTION REFERENCE
     * =========================================================
     *
     * Useful for:
     *
     * UPI transaction ID
     * Bank reference number
     * Cheque number
     *
     * =========================================================
     */

    @Column(
            name = "transaction_reference",
            length = 100
    )
    private String transactionReference;


    /*
     * =========================================================
     * NOTES
     * =========================================================
     */

    @Column(columnDefinition = "TEXT")
    private String notes;


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
     * PRE-PERSIST
     * =========================================================
     */

    @PrePersist
    protected void onCreate() {

        LocalDateTime now = LocalDateTime.now();

        if (paymentDate == null) {
            paymentDate = now;
        }

        createdAt = now;
    }
}