package com.shivhub.backend.enums;

/** Non-GST shop cashbook movements. These never post to the tax or inventory ledgers. */
public enum ShopRegisterEntryType {
    CASH_SALE,
    ONLINE_SALE,
    EXPENSE,
    CASH_DEPOSIT,
    CASH_WITHDRAWAL,
    ADJUSTMENT
}
