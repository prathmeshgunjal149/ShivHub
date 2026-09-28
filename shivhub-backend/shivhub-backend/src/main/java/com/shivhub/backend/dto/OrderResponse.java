package com.shivhub.backend.dto;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

import com.shivhub.backend.enums.OrderStatus;
import com.shivhub.backend.enums.PaymentMethod;
import com.shivhub.backend.enums.PaymentStatus;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;


/*
 * =========================================================
 * OrderResponse
 * =========================================================
 *
 * Response DTO used for:
 *
 * 1. Customer My Orders
 * 2. Customer Order Details
 * 3. Admin Orders
 * 4. Admin Order Details
 * 5. Order cancellation information
 *
 * =========================================================
 */

@Data
@NoArgsConstructor
@AllArgsConstructor
public class OrderResponse {


    /*
     * =========================================================
     * ORDER INFORMATION
     * =========================================================
     */

    private Long orderId;

    private String orderNumber;


    /*
     * =========================================================
     * CUSTOMER INFORMATION
     * =========================================================
     */

    private Long customerId;

    private String customerName;

    private String customerEmail;


    /*
     * =========================================================
     * ORDER ITEMS
     * =========================================================
     */

    private List<OrderItemResponse> items;

    /** One customer-visible promise per seller participating in the order. */
    private List<OrderDeliveryExpectationResponse> deliveryExpectations;


    /*
     * =========================================================
     * ORDER AMOUNTS
     * =========================================================
     */

    private BigDecimal subtotal;

    private BigDecimal discount;

    private BigDecimal tax;

    private BigDecimal deliveryCharge;

    private long loyaltyPointsRedeemed;

    private BigDecimal loyaltyDiscount;

    private BigDecimal grandTotal;


    /*
     * =========================================================
     * ORDER STATUS
     * =========================================================
     */

    private OrderStatus orderStatus;

    private OrderStatus sellerRequestedStatus;
    private LocalDateTime sellerStatusRequestedAt;
    private String deliveryPersonName;
    private String deliveryPersonMobile;

    private PaymentStatus paymentStatus;

    private PaymentMethod paymentMethod;


    /*
     * =========================================================
     * SHIPPING
     * =========================================================
     */

    private String shippingAddress;
    private String deliveryName;
    private String deliveryMobile;
    private String deliveryAddressLine1;
    private String deliveryAddressLine2;
    private String deliveryLandmark;
    private String deliveryCity;
    private String deliveryDistrict;
    private String deliveryState;
    private String deliveryPincode;


    /*
     * =========================================================
     * CANCELLATION INFORMATION
     * =========================================================
     *
     * These fields are populated only when
     * an order is cancelled.
     *
     * cancellationReason
     * -------------------
     * Main reason selected by customer.
     *
     *
     * cancellationComment
     * --------------------
     * Optional additional explanation.
     *
     *
     * cancelledBy
     * -----------
     * CUSTOMER
     * ADMIN
     *
     *
     * cancelledAt
     * -----------
     * Date and time of cancellation.
     *
     * =========================================================
     */

    private String cancellationReason;

    private String cancellationComment;

    private String cancelledBy;

    private LocalDateTime cancelledAt;


    /*
     * =========================================================
     * DATE / TIME
     * =========================================================
     */

    private LocalDateTime createdAt;

    private LocalDateTime updatedAt;
}
