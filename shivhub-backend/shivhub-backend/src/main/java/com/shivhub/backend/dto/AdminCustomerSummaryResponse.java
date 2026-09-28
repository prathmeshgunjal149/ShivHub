package com.shivhub.backend.dto;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

/** A combined online/walk-in customer view; no seller-private data is exposed to another seller. */
public record AdminCustomerSummaryResponse(
        Long id, Long onlineUserId, String name, String mobile, String email, String address,
        String customerType, List<String> associatedSellers, long totalOrdersAndBills,
        BigDecimal totalPurchaseAmount, BigDecimal pendingReceivable,
        LocalDateTime lastPurchaseDate, boolean communicationConsent, boolean accountEnabled
) { }
