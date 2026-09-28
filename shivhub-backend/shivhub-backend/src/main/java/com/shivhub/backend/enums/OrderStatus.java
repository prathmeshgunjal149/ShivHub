package com.shivhub.backend.enums;

/*
 * =========================================================
 * OrderStatus
 * =========================================================
 *
 * Represents the current status of an order.
 *
 * =========================================================
 */

public enum OrderStatus {

    PENDING,

    CONFIRMED,

    PROCESSING,

    PACKED,

    SHIPPED,

    OUT_FOR_DELIVERY,

    DELIVERED,

    CANCELLED
}
