package com.shivhub.backend.entity;

import java.math.BigDecimal;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;


/*
 * =========================================================
 * OrderItem Entity
 * =========================================================
 *
 * Represents one product inside an order.
 *
 * Example:
 *
 * Order #1001
 *
 *    iPhone 15       quantity = 1
 *    Boat Earbuds    quantity = 2
 *
 * Each product becomes one OrderItem.
 *
 * =========================================================
 */

@Entity
@Table(name = "order_items")

@Data
@NoArgsConstructor
@AllArgsConstructor

public class OrderItem {
    @Column(name = "product_variant_id") private Long productVariantId;
    @Column(name = "selected_attributes", columnDefinition = "TEXT") private String selectedAttributes;
    @Column(name = "delivery_estimate_text", length = 500) private String deliveryEstimateText;
    @Column(name = "delivery_distance_km", precision = 10, scale = 2) private BigDecimal deliveryDistanceKm;
    @Column(name = "delivery_rule_id") private Long deliveryRuleId;


    /*
     * =========================================================
     * PRIMARY KEY
     * =========================================================
     */

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;


    /*
     * =========================================================
     * ORDER
     * =========================================================
     *
     * Many OrderItems belong to one Order.
     *
     */

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(
        name = "order_id",
        nullable = false
    )
    private Order order;


    /*
     * =========================================================
     * PRODUCT ID
     * =========================================================
     *
     * We store Product ID for now.
     *
     * This keeps the entity independent from your exact
     * existing Product relationship until we inspect it.
     *
     */

    @Column(
        name = "product_id",
        nullable = false
    )
    private Long productId;


    /*
     * =========================================================
     * PRODUCT NAME SNAPSHOT
     * =========================================================
     *
     * Important:
     *
     * If product name changes later, old order should still
     * show the name that existed when the order was placed.
     *
     */

    @Column(
        name = "product_name",
        nullable = false
    )
    private String productName;

    /** Seller identity at the time of sale; retained for audit after profile changes. */
    @Column(name = "seller_id")
    private Long sellerId;

    @Column(name = "seller_name")
    private String sellerName;

    @Column(name = "seller_gstin", length = 15)
    private String sellerGstin;

    @Column(name = "seller_address", columnDefinition = "TEXT") private String sellerAddress;
    @Column(name = "seller_mobile", length = 15) private String sellerMobile;
    @Column(name = "seller_warranty_period", length = 100) private String sellerWarrantyPeriod;
    @Column(name = "seller_warranty_type", length = 50) private String sellerWarrantyType;
    @Column(name = "seller_warranty_terms", columnDefinition = "TEXT") private String sellerWarrantyTerms;
    @Column(name = "seller_return_policy", columnDefinition = "TEXT") private String sellerReturnPolicy;


    /*
     * =========================================================
     * PRODUCT PRICE SNAPSHOT
     * =========================================================
     *
     * Stores price at the time of purchase.
     *
     */

    @Column(
        name = "unit_price",
        precision = 12,
        scale = 2,
        nullable = false
    )
    private BigDecimal unitPrice;


    /*
     * =========================================================
     * QUANTITY
     * =========================================================
     */

    @Column(
        name = "quantity",
        nullable = false
    )
    private Integer quantity;


    /*
     * =========================================================
     * TOTAL PRICE
     * =========================================================
     *
     * unitPrice × quantity
     *
     */

    @Column(
        name = "total_price",
        precision = 12,
        scale = 2,
        nullable = false
    )
    private BigDecimal totalPrice;

    @Column(name = "assigned_serial_summary", columnDefinition = "TEXT")
    private String assignedSerialSummary;
}
