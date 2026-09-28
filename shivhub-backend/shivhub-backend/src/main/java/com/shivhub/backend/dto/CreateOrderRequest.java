package com.shivhub.backend.dto;

import java.util.List;

import com.shivhub.backend.enums.PaymentMethod;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Min;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;


/*
 * =========================================================
 * CreateOrderRequest
 * =========================================================
 *
 * Request received when a customer places an order.
 *
 * IMPORTANT:
 *
 * customerId is NOT received from frontend.
 *
 * Logged-in customer is identified using:
 *
 * JWT
 *  ↓
 * Authentication
 *  ↓
 * Email
 *  ↓
 * UserRepository
 *
 * =========================================================
 */

@Data
@NoArgsConstructor
@AllArgsConstructor
public class CreateOrderRequest {


    /*
     * =========================================================
     * SHIPPING ADDRESS
     * =========================================================
     */

    private String shippingAddress;

    /** Existing saved address, verified against the authenticated customer. */
    private Long deliveryAddressId;

    /** First-order address; saved atomically only when the order succeeds. */
    @Valid
    private CustomerAddressRequest deliveryAddress;


    /*
     * =========================================================
     * ORDER ITEMS
     * =========================================================
     *
     * At least one item is required.
     *
     * @Valid is applied to the list element type.
     *
     * This avoids the Hibernate Validator warning:
     *
     * HV000271
     *
     * =========================================================
     */

    @NotEmpty(message = "Order must contain at least one item")
    private List<@Valid OrderItemRequest> items;

    /** Optional code supplied at checkout. It is always revalidated on the server. */
    private String couponCode;

    /** Optional whole loyalty points to redeem. The authenticated customer's balance is checked again on the server. */
    @Min(value = 0, message = "Loyalty points cannot be negative")
    private Integer loyaltyPointsToRedeem;

    /** Customer's preferred collection method. It never marks an order as paid. */
    private PaymentMethod paymentMethod;

}
