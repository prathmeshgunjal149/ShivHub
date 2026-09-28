package com.shivhub.backend.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;


/*
 * =========================================================
 * AddToCartRequest
 * =========================================================
 *
 * Request used when customer adds a product to cart.
 *
 * Example:
 *
 * {
 *     "productId": 1,
 *     "quantity": 2
 * }
 *
 * =========================================================
 */

@Data
@NoArgsConstructor
@AllArgsConstructor
public class AddToCartRequest {


    /*
     * =====================================================
     * PRODUCT ID
     * =====================================================
     */

    @NotNull(message = "Product ID is required")
    private Long productId;


    /*
     * =====================================================
     * QUANTITY
     * =====================================================
     *
     * Minimum quantity = 1
     *
     * =====================================================
     */

    @NotNull(message = "Quantity is required")
    @Min(
        value = 1,
        message = "Quantity must be at least 1"
    )
    private Integer quantity;

    /** Optional non-mobile variant; validated server-side against the product. */
    private Long variantId;
}
