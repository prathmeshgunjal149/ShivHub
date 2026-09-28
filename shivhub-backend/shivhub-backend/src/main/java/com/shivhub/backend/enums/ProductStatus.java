package com.shivhub.backend.enums;


/*
 * =========================================================
 * ProductStatus
 * =========================================================
 *
 * Controls the admin approval status of a product.
 *
 * PENDING
 *   Seller has submitted the product.
 *   Waiting for admin approval.
 *
 * APPROVED
 *   Admin has approved the product.
 *   Product can be shown to customers
 *   when active = true.
 *
 * REJECTED
 *   Admin has rejected the product.
 *
 * =========================================================
 */

public enum ProductStatus {

    PENDING,

    APPROVED,

    REJECTED,

    /** Product is temporarily hidden by an administrator. */
    SUSPENDED
}
