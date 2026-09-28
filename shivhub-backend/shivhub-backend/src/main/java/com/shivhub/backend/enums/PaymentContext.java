package com.shivhub.backend.enums;

/** Identifies the business record a payment transaction settles. */
public enum PaymentContext {
    ONLINE_ORDER,
    OFFLINE_BILL,
    CUSTOMER_RECEIVABLE_PAYMENT,
    DISTRIBUTOR_PAYMENT
}
