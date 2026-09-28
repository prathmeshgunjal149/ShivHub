package com.shivhub.backend.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;


/*
 * =========================================================
 * OrderItemRequest
 * =========================================================
 *
 * Represents one product that the customer wants to order.
 *
 * =========================================================
 */

@Data
@NoArgsConstructor
@AllArgsConstructor
public class OrderItemRequest {
    private Long variantId;

    @NotNull
    private Long productId;

    @NotNull
    @Min(1)
    private Integer quantity;
}
