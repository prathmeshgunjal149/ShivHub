package com.shivhub.backend.dto;

import java.time.LocalDateTime;

/** Customer-safe immutable ledger row. It intentionally excludes seller cost and internal data. */
public record CustomerLoyaltyTransactionResponse(
        Long id,
        String transactionType,
        String sourceType,
        Long sourceId,
        long points,
        long balanceAfter,
        String remarks,
        LocalDateTime createdAt
) { }
