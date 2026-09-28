package com.shivhub.backend.entity;

import java.time.LocalDateTime;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import jakarta.persistence.Version;
import lombok.Getter;
import lombok.Setter;

/**
 * Customer-visible delivery promise for one seller's part of an online order.
 * It deliberately does not contain courier cost, internal notes or staff data.
 */
@Entity
@Table(name = "order_delivery_expectations",
        uniqueConstraints = @UniqueConstraint(name = "uk_order_delivery_expectation_seller", columnNames = {"order_id", "seller_id"}),
        indexes = {@Index(name = "idx_order_delivery_expectation_order", columnList = "order_id"),
                @Index(name = "idx_order_delivery_expectation_seller", columnList = "seller_id,expected_delivery_at")})
@Getter
@Setter
public class OrderDeliveryExpectation {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "order_id", nullable = false)
    private Long orderId;

    @Column(name = "seller_id", nullable = false)
    private Long sellerId;

    @Column(name = "seller_name", length = 160)
    private String sellerName;

    @Column(name = "expected_delivery_at", nullable = false)
    private LocalDateTime expectedDeliveryAt;

    @Column(name = "customer_message", length = 500)
    private String customerMessage;

    @Column(name = "updated_by_role", nullable = false, length = 20)
    private String updatedByRole;

    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    @Version
    private Long version;
}
