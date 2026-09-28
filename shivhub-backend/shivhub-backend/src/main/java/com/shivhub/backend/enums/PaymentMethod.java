package com.shivhub.backend.enums;


/*
 * =========================================================
 * PaymentMethod
 * =========================================================
 *
 * Payment methods supported by ShivHub Offline Billing /
 * POS system.
 *
 * =========================================================
 */

public enum PaymentMethod {

    /*
     * Cash payment
     */
    CASH,

    /*
     * UPI payment
     */
    UPI,

    /*
     * Debit / Credit Card
     */
    CARD,

    /* Online banking through a gateway such as Razorpay. */
    NET_BANKING,

    /*
     * Bank transfer
     */
    BANK_TRANSFER,

    CHEQUE,

    /* Gateway-backed UPI/Card/NetBanking collection. */
    RAZORPAY,

    OTHER,

    FINANCE,

    /*
     * More than one payment method used
     *
     * Example:
     * ₹5,000 Cash
     * ₹10,000 UPI
     */
    MIXED
}
