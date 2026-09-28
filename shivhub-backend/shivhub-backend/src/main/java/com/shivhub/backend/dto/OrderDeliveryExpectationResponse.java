package com.shivhub.backend.dto;

import java.time.LocalDateTime;

/** Safe for customer, seller and admin UIs. */
public record OrderDeliveryExpectationResponse(
        Long sellerId,
        String sellerName,
        LocalDateTime expectedDeliveryAt,
        String customerMessage,
        String updatedByRole,
        LocalDateTime updatedAt
) { }
