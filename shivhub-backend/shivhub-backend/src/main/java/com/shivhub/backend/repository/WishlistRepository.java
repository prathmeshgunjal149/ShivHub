package com.shivhub.backend.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.shivhub.backend.entity.Wishlist;


/*
 * =========================================================
 * WishlistRepository
 * =========================================================
 *
 * Communicates with:
 *
 *     wishlist
 *
 * table in MySQL.
 *
 * Used for:
 *
 * 1. Add wishlist item
 * 2. Find customer's wishlist
 * 3. Check whether product is already in wishlist
 * 4. Remove product from wishlist
 *
 * =========================================================
 */

@Repository
public interface WishlistRepository
        extends JpaRepository<Wishlist, Long> {


    /*
     * =====================================================
     * GET CUSTOMER WISHLIST
     * =====================================================
     */

    List<Wishlist> findByCustomerIdOrderByCreatedAtDesc(
            Long customerId
    );


    /*
     * =====================================================
     * CHECK PRODUCT ALREADY EXISTS
     * =====================================================
     *
     * Prevents the same customer from adding
     * the same product multiple times.
     *
     * =====================================================
     */

    boolean existsByCustomerIdAndProductId(
            Long customerId,
            Long productId
    );


    /*
     * =====================================================
     * FIND SPECIFIC WISHLIST ITEM
     * =====================================================
     */

    Optional<Wishlist> findByCustomerIdAndProductId(
            Long customerId,
            Long productId
    );


    /*
     * =====================================================
     * DELETE SPECIFIC PRODUCT
     * =====================================================
     */

    void deleteByCustomerIdAndProductId(
            Long customerId,
            Long productId
    );


    /*
     * =====================================================
     * COUNT CUSTOMER WISHLIST
     * =====================================================
     */

    long countByCustomerId(
            Long customerId
    );
}