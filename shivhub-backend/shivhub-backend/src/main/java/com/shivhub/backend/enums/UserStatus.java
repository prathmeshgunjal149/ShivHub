package com.shivhub.backend.enums;


/*
 * =========================================================
 * UserStatus
 * =========================================================
 *
 * This enum represents the current status of a ShivHub
 * user account.
 *
 * CUSTOMER:
 *     Normally APPROVED immediately.
 *
 * SELLER:
 *     Starts as PENDING.
 *     Admin must approve the seller.
 *
 * ADMIN:
 *     APPROVED.
 *
 * =========================================================
 */

public enum UserStatus {

    /*
     * Seller application is waiting for Admin approval.
     */
    PENDING,


    /*
     * Account is approved and can use the system.
     */
    APPROVED,


    /*
     * Admin rejected the seller application.
     */
    REJECTED,

    /** Account is retained for audit but cannot access ShivHub. */
    SUSPENDED
}
