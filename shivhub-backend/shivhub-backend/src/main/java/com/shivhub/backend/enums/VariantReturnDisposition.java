package com.shivhub.backend.enums;

/**
 * Final physical disposition for a returned non-mobile product variant.
 * Only AVAILABLE restores sellable stock; all other outcomes remain
 * non-sellable and are retained in the after-sales audit ledger.
 */
public enum VariantReturnDisposition {
    AVAILABLE,
    DEFECTIVE,
    SERVICE_CENTER,
    SCRAP,
    RETURNED_TO_DISTRIBUTOR
}
