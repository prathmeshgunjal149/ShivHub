package com.shivhub.backend.dto;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/** Compact response for distributor-payable screens; avoids serialising the full circular purchase graph. */
public record DistributorPayableBillResponse(Long id, String invoiceNumber, LocalDateTime purchaseDate,
        BigDecimal grandTotal, String distributorName) { }
