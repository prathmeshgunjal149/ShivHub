package com.shivhub.backend.dto;

import java.time.LocalDateTime;

import jakarta.validation.constraints.FutureOrPresent;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

/** Seller/admin request. Seller identity is always derived from JWT, never this payload. */
public record OrderDeliveryExpectationRequest(
        @NotNull @FutureOrPresent LocalDateTime expectedDeliveryAt,
        @Size(max = 500) String customerMessage
) { }
