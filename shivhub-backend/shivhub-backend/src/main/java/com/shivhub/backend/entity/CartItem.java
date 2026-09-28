package com.shivhub.backend.entity;

import java.time.LocalDateTime;

import com.fasterxml.jackson.annotation.JsonIgnore;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;


/*
 * =========================================================
 * CartItem Entity
 * =========================================================
 *
 * Represents one product inside a customer's cart.
 *
 * Example:
 *
 * Customer ID = 1
 *
 * Cart:
 *
 * iPhone 15       quantity = 2
 * Samsung S24     quantity = 1
 *
 * Each product is stored as a separate CartItem.
 *
 * =========================================================
 */

@Entity
@Table(
    name = "cart_items",
    uniqueConstraints = {
        @jakarta.persistence.UniqueConstraint(
            name = "uk_cart_customer_selection",
            columnNames = {
                "customer_id",
                "product_id", "selection_key"
            }
        )
    }
)
@Data
@NoArgsConstructor
@AllArgsConstructor
public class CartItem {


    /*
     * =====================================================
     * PRIMARY KEY
     * =====================================================
     */

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;


    /*
     * =====================================================
     * CUSTOMER
     * =====================================================
     *
     * Many cart items belong to one customer.
     *
     * Example:
     *
     * Customer 1
     *    |
     *    ├── CartItem 1
     *    ├── CartItem 2
     *    └── CartItem 3
     *
     * JsonIgnore prevents User information from being
     * returned inside the cart response.
     *
     * =====================================================
     */

    @JsonIgnore
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(
            name = "customer_id",
            nullable = false
    )
    private User customer;


    /*
     * =====================================================
     * PRODUCT
     * =====================================================
     *
     * Many cart items can reference products.
     *
     * =====================================================
     */

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(
            name = "product_id",
            nullable = false
    )
    private Product product;

    @JsonIgnore
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "product_variant_id")
    private ProductVariant productVariant;

    @JsonIgnore
    @Column(name = "selection_key", nullable = false)
    private Long selectionKey = 0L;

    public Long getVariantId() { return productVariant == null ? null : productVariant.getId(); }
    public java.math.BigDecimal getUnitPrice() { return productVariant == null ? product.getFinalSellingPrice() : productVariant.getSellingPriceIncludingGst(); }
    public int getAvailableStock() { return productVariant == null ? product.getAvailableStock() : productVariant.getAvailableStock(); }
    public String getSelectedAttributes() { return productVariant == null ? null : productVariant.getAttributesJson(); }
    public String getVariantImageUrl() { return productVariant == null ? null : productVariant.getImageUrl(); }


    /*
     * =====================================================
     * QUANTITY
     * =====================================================
     */

    @Column(nullable = false)
    private Integer quantity;


    /*
     * =====================================================
     * CREATED AT
     * =====================================================
     */

    @Column(
            nullable = false,
            updatable = false
    )
    private LocalDateTime createdAt;


    /*
     * =====================================================
     * UPDATED AT
     * =====================================================
     */

    @Column(nullable = false)
    private LocalDateTime updatedAt;


    /*
     * =====================================================
     * BEFORE INSERT
     * =====================================================
     */

    @PrePersist
    protected void onCreate() {

        createdAt = LocalDateTime.now();

        updatedAt = LocalDateTime.now();
    }


    /*
     * =====================================================
     * BEFORE UPDATE
     * =====================================================
     */

    @PreUpdate
    protected void onUpdate() {

        updatedAt = LocalDateTime.now();
    }
}
