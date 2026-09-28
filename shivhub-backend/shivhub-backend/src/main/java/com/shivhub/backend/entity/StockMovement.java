package com.shivhub.backend.entity;

import java.time.LocalDateTime;

import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;


/*
 * =========================================================
 * STOCK MOVEMENT
 * =========================================================
 *
 * Keeps complete history of product stock changes.
 *
 * Example:
 *
 * PURCHASE
 * Stock 10 → 15
 *
 * SALE
 * Stock 15 → 14
 *
 * PURCHASE_CANCEL
 * Stock 15 → 10
 *
 * =========================================================
 */

@Entity
@Table(name = "stock_movements")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class StockMovement {
    @jakarta.persistence.Column(name = "product_variant_id")
    private Long productVariantId;


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
     * PRODUCT
     * =========================================================
     *
     * Which product's stock changed.
     *
     * Example:
     *
     * Product ID = 17
     *
     * =========================================================
     */

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(
            name = "product_id",
            nullable = false
    )
    private Product product;


    /*
     * =========================================================
     * SELLER
     * =========================================================
     *
     * Stock belongs to a particular seller.
     *
     * =========================================================
     */

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(
            name = "seller_id",
            nullable = false
    )
    private User seller;


    /*
     * =========================================================
     * MOVEMENT TYPE
     * =========================================================
     *
     * Examples:
     *
     * PURCHASE
     * SALE
     * PURCHASE_CANCEL
     * SALE_RETURN
     * STOCK_ADJUSTMENT
     *
     * =========================================================
     */

    @Column(
            name = "movement_type",
            nullable = false,
            length = 50
    )
    private String movementType;


    /*
     * =========================================================
     * QUANTITY
     * =========================================================
     *
     * Positive quantity means stock added.
     *
     * Negative quantity means stock removed.
     *
     * Example:
     *
     * +5
     * -2
     *
     * =========================================================
     */

    @Column(
            nullable = false
    )
    private Integer quantity;


    /*
     * =========================================================
     * STOCK BEFORE
     * =========================================================
     */

    @Column(
            name = "stock_before",
            nullable = false
    )
    private Integer stockBefore;


    /*
     * =========================================================
     * STOCK AFTER
     * =========================================================
     */

    @Column(
            name = "stock_after",
            nullable = false
    )
    private Integer stockAfter;


    /*
     * =========================================================
     * REFERENCE TYPE
     * =========================================================
     *
     * Example:
     *
     * PURCHASE
     * SALE
     *
     * =========================================================
     */

    @Column(
            name = "reference_type",
            length = 50
    )
    private String referenceType;


    /*
     * =========================================================
     * REFERENCE ID
     * =========================================================
     *
     * Example:
     *
     * Purchase ID = 4
     *
     * referenceType = PURCHASE
     * referenceId   = 4
     *
     * =========================================================
     */

    @Column(
            name = "reference_id"
    )
    private Long referenceId;


    /*
     * =========================================================
     * NOTES
     * =========================================================
     */

    @Column(
            length = 500
    )
    private String notes;


    /*
     * =========================================================
     * CREATED AT
     * =========================================================
     */

    @Column(
            name = "created_at",
            nullable = false
    )
    private LocalDateTime createdAt;


    /*
     * =========================================================
     * PRE PERSIST
     * =========================================================
     */

    @PrePersist
    protected void onCreate() {

        if (createdAt == null) {

            createdAt =
                    LocalDateTime.now();
        }
    }
}
