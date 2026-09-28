package com.shivhub.backend.dto;

import java.math.BigDecimal;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;


/*
 * =========================================================
 * OrderItemResponse
 * =========================================================
 *
 * Represents one product inside an order response.
 *
 * =========================================================
 */

@Data
@NoArgsConstructor
public class OrderItemResponse {
    private Long variantId;
    private String selectedAttributes;
    private String deliveryEstimateText;
    private BigDecimal deliveryDistanceKm;
    private Long sellerId;
    private String sellerName;
    /** Current product image for customer order-history cards; null is valid for legacy products. */
    private String imageUrl;
    public OrderItemResponse(Long id, Long productId, String productName, BigDecimal unitPrice, Integer quantity, BigDecimal totalPrice, String assignedSerialSummary) {
        this.id=id; this.productId=productId; this.productName=productName; this.unitPrice=unitPrice; this.quantity=quantity; this.totalPrice=totalPrice; this.assignedSerialSummary=assignedSerialSummary;
    }

    private Long id;

    private Long productId;

    private String productName;

    private BigDecimal unitPrice;

    private Integer quantity;

    private BigDecimal totalPrice;

    private String assignedSerialSummary;
}
