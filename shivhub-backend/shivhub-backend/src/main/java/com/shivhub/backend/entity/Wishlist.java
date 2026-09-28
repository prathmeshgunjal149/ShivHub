package com.shivhub.backend.entity;

import java.time.LocalDateTime;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;


/*
 * =========================================================
 * Wishlist Entity
 * =========================================================
 *
 * Stores products saved by customers.
 *
 * Example:
 *
 * Customer 11
 *      ↓
 * ❤️ iPhone 15
 * ❤️ Samsung S21
 *
 * Database table:
 *
 * wishlist
 *
 * =========================================================
 */

@Entity
@Table(name = "wishlist")

@Data
@NoArgsConstructor
@AllArgsConstructor

public class Wishlist {


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
     * CUSTOMER ID
     * =====================================================
     *
     * We store the User ID of the customer.
     *
     * =====================================================
     */

    @Column(
            name = "customer_id",
            nullable = false
    )
    private Long customerId;


    /*
     * =====================================================
     * PRODUCT ID
     * =====================================================
     *
     * Stores the product saved by customer.
     *
     * =====================================================
     */

    @Column(
            name = "product_id",
            nullable = false
    )
    private Long productId;


    /*
     * =====================================================
     * CREATED AT
     * =====================================================
     */

    @Column(
            name = "created_at",
            nullable = false,
            updatable = false
    )
    private LocalDateTime createdAt;


    /*
     * =====================================================
     * PRE-PERSIST
     * =====================================================
     */

    @PrePersist
    protected void onCreate() {

        createdAt = LocalDateTime.now();
    }
}